### Calibration Slide
Plain text will be at most this small.

```scala 3
// Code will look like this

@main def hello: Unit = println("Hello world!")
```

If you are having trouble seeing, make your window bigger,
or let me know if I should switch to a higher contrast theme.

Talk Channel: #talk-practical-typeclass-usage

---

### Practical Typeclass Usage
#### Improving Java Library Ergonomics

Paul (Thor) Thordarson

---

### Who Am I

* Ran into Scala about 8 years ago and fell in love
* Joined a new team at work using Scala with Spark
* Started writing support Slack bots in Akka
* Took a job at a startup using FP Scala
* Recently started contributing to core Scala

Note: Talk a little bit about being the senior person on a team
where no one had Scala experience. Also mention that contributing
is not as scary as it first seems, and encourage people to give
it a try.

---

### About this talk

* Aimed at relative newcomers to Scala.
* Will be given in Scala 3
* Assumes you mostly know how `given` works

Note: Talk about how this particular problem helped make Typeclasses in particular click for me as well as
really move me over from writing _better Java_ to idiomatic Scala. Also mention that this is a more simple
example, should give you the understanding to tackle something more complex like the typeclasses shown in
Michael's talk.

---

### What's a Typeclass?

* FP interface for supporting _ad-hoc polymorphism_.
* For common behavior between unrelated types:
  * Comparison
  * Encoding/decoding instances to various codecs
  * Composition of types
* Extends a types behaviour without extending the type itself.

Note: You don't need to control the source code of the class being extended. Useful for working with
legacy or Java libraries.

---

### Representing Typeclasses in Scala

* Typeclasses are borrowed from Haskell
* Typeclasses in Scala are not first-class entities
* Typeclasses are encoded in traits
  * All typeclasses are traits
  * Not all traits are typeclasses

Note: Emphasize that this is an important distinction, just because you see a trait in a given with a type parameter

---

### More about Typeclasses

* Typeclasses should be canonical
* Define behavior that is always true
* Typeclasses usually defined in libraries
* Should rarely if ever define business logic

Note: Does have a place in enterprise code, it's usually found in
your nuts and bolts libraries and glue code. Does this mean it doesn't
belong in application code? Not at all, large projects often have library.
Make sure to talk about experience with library. Also mention that library
authors use typeclasses with the intent of making integration easy for
other developers.

---
_Typeclasses should never be subject to product requirements!_
---

### Where are typeclasses found?

* Standard Library - `Ordering[A]`
* Spark - `Encoder[A]`
* All over the Typelevel ecosystem
  * cats `Monoid`, `Applicative`, & `Functor`
  * cats-effect `Async` & `Concurrent`
  * circe `Encoder` & `Decoder`

Note: Good overview from Adam's talk

---

### Our Java Config Library
Lightbend (formerly Typesafe) Config

_What We Have - Java API_
<!-- .element: class="fragment" data-fragment-index="1" -->

```java
public interface Config {
    boolean getBoolean(String path);
    double getDouble(String path);
    List<Integer> getIntList(String path);
    . . .
}
```
<!-- .element: class="fragment" data-fragment-index="1" -->

_What We'd Like - Scala API_
<!-- .element: class="fragment" data-fragment-index="2" -->

```scala 3
trait Config {
  def get[T](path: String): T
}
```
<!-- .element: class="fragment" data-fragment-index="2" -->

Note: Start by talking about the convenience of having access to the Java ecosystem.
We'd like to ergonomics that are more idiomatically Scala, something that is generic, but also with type safety.

---
### Building our API

The Typeclass
<!-- .element: class="fragment" data-fragment-index="1" -->

```scala 3
@implicitNotFound("Missing given Extractor for type ${T}")
trait Extractor[T] {
  def extract(config: Config, path: String): T
}
```
<!-- .element: class="fragment" data-fragment-index="1" -->

The `Config` extension
<!-- .element: class="fragment" data-fragment-index="2" -->

```scala 3
extension (config: Config)
  def get[T](path: String)(using ex: Extractor[T]): T =
    ex.extract(config, path)
```
<!-- .element: class="fragment" data-fragment-index="2" -->

Note: Important to note that our extension uses our typeclass,
but is not an essential part of the typeclass itself. Pause to
mention this. Mention Michael's talk and point out difference
between extensions defined on the typeclass and why this is different.

---

### Our First Typeclass

```scala 3
object Extractor {
  given Extractor[String] with
    def extract(config: Config, path: String): String =
      config.getString(path)
}
```

Alternate Syntax
```scala 3
object Extractor {
  given Extractor[String] =
    (config, path) => config.getString(path)
}
```

Note: Alternate Syntax - SAM -- Single Abstract Method.
Mention that I will mostly be using this more concise syntax.

---

### First Pass

```scala 3
object Extractor {
  given Extractor[String] =
    (config, path) => config.getString(path)
  given Extractor[Int] =
    (config, path) => config.getInt(path)
  given Extractor[Long] =
    (config, path) => config.getLong(path)
  given Extractor[Double] =
    (config, path) => config.getDouble(path)
  given Extractor[Boolean] =
    (config, path) => config.getBoolean(path)
}
```

Note: Mention that simple cases like AnyVal are usually all written by library authors when you come on them,
as well as any typeclasses, like you may have seen in Katrix's talk.

---

### What does this get us?

Before:
```scala 3
  val host = config.getString("host")
  val port = config.getInt("port")
  val useHttps = config.getBoolean("use-https")
```

After:
<!-- .element: class="fragment" data-fragment-index="1" -->
```scala 3
  val host = config.get[String]("host")
  val port = config.get[Int]("port")
  val useHttps = config.get[Boolean]("use-https")
```
<!-- .element: class="fragment" data-fragment-index="1" -->

Note: Not much, but it does look a little more idiomatically Scala

---

### Custom Typeclass Extractor

```scala 3
case class RestConfig(
             host: String,
             port: Int,
             useHttps: Boolean)

object RestConfig {
  given Extractor[RestConfig] = (config, path) => {
    val c = config.getConfig(path)
    val host = c.get[String]("host")
    val port = c.get[Int]("port")
    val useHttps = c.get[Boolean]("use-https")

    RestConfig(host, port, useHttps)
  }
}
```

Note: Talk about re-usable nature here. Mention extractor in companion object.

---

### DRY with the Extractor

```
service-a {
  host = "somerestapi.com"
  port = 9000
  use-https = true
}
service-b {
  host = "anotherrestapi.com"
  port = 7777
  use-https = false
}
```

Usage is simplified
<!-- .element: class="fragment" data-fragment-index="1" -->

```scala 3
val serviceAConf: RestConfig =
  config.get[RestConfig]("service-a")
val serviceBConf: RestConfig =
  config.get[RestConfig]("service-b")
```
<!-- .element: class="fragment" data-fragment-index="1" -->

---

### What about optional members?

The library throws `ConfigException.Missing`

Raw code:
<!-- .element: class="fragment" data-fragment-index="1" -->

```scala 3
val port: Option[Int] =
  if (config.hasPath("port")) {
    Some(config.get[Int]("port"))
  } else {
    None
  }
```
<!-- .element: class="fragment" data-fragment-index="1" -->

What we'd like:
<!-- .element: class="fragment" data-fragment-index="2" -->

```scala 3
val port: Option[Int] = config.get[Option[Int]]("port")
```
<!-- .element: class="fragment" data-fragment-index="2" -->

Note: Mention being told to take all my `Optional<T>` types
out of my code.

---

### The Solution: Typeclasses can be Inductive

The Option Typeclass
<!-- .element: class="fragment" data-fragment-index="1" -->

```scala 3
object Extractor {
  given optExtractor[T](
      using ex: Extractor[T]): Extractor[Option[T]] =
    (config, path) => {
      if (config.hasPath(path)) {
        Some(ex.extract(config, path))
      } else {
        None
      }
    }
}
```
<!-- .element: class="fragment" data-fragment-index="1" -->

Note: As you may have noticed in Michael's talk, Typeclasses can be
built with other typclasses.

---

### Now we can do the following

```scala 3
  val host = config.get[String]("host")
  val port = config.get[Option[Int]]("port")
  val useHttps = config.get[Option[Boolean]]("use-https")
```

Note: This is much nicer. We've taken some of the implementation details and hidden them.
Anyone using the option doesn't need to understand how we get them, it just works.

---

### Another Extension Method

```scala 3
extension (config: Config)
  . . .
  
  def getOrElse[T: Extractor](path: String, default: T): T =
    get[Option[T]](path).getOrElse(default)
```

```scala 3
object RestConfig {
  given Extractor[RestConfig] = (config, path) => {
    val c = config.getConfig(path)
    val host = c.get[String]("host")
    val port = c.getOrElse[Int]("port", 8080)
    val useHttps = c.getOrElse[Boolean]("use-https", false)

    RestConfig(host, port, useHttps)
  }
}
```
<!-- .element: class="fragment" data-fragment-index="1" -->

Note: We can now rely on our `Option[_]` Extractor to improve the API to make a simple getOrElse. We can modify
our RestConfig to handle sensible defaults.

---

### Another Higher-kinded Type: List

```scala 3
object Extractor {
  given stringListExtractor: Extractor[List[String]] =
    (config, path) => config
      .getStringList(path)
      .asScala.toList

  given intListExtractor: Extractor[List[Int]] =
    (config, path) => config
      .getIntList(path)
      .asScala.toList.map(_.toInt)
    
  given longListExtractor: Extractor[List[Long]] = ...
  given doubleListExtractor: Extractor[List[Double]] = ...
  given booleanListExtractor: Extractor[List[Boolean]] = ...
}
```
<!-- .element: class="fragment" data-fragment-index="1" -->

Note: We can still define higher kinded types explicitly. Note in this case our library returns Java
collection types, and we'd really rather not worry about conversion. We also need to handle boxed java type
conversions.

---
### What about Complex types?
```
services = [{
  host = "foo.net"
  port = 1234
}, {
  host = "bar.org"
  use-https = true
}]
```

<!-- .element: class="fragment" data-fragment-index="1" -->
```java
public interface Config {
    . . .
    List<? extends Config> getConfigList(String path);
    . . .
}
```
<!-- .element: class="fragment" data-fragment-index="1" -->

Problem: Our typeclass requires a path, array elements don't have a path.
<!-- .element: class="fragment" data-fragment-index="1" -->

---
### The solution: A hack

```scala 3
given listExtractor[T](
    using ex: Extractor[T]): Extractor[List[T]] =
  (config, path) => {
    def wrap(conf: Config): Config = {
      val wrappedConfig =
        Map("key" -> conf.root().unwrapped()).asJava
      ConfigValueFactory
        .fromMap(wrappedConfig).toConfig
    }
    config.getConfigList(path)
      .asScala.toList.map(c => ex.extract(wrap(c), "key"))
  }
```

Note: Ugly but works. Wrap the Config elements before passing them to the Extractor typeclass.
Hides the messiness from users of the library. Is this perfect? No, but it works and 

---

### Using List extractors
Uses `Extractor[List[Int]]`
<!-- .element: class="fragment" data-fragment-index="1" -->
```scala 3
// retry-periods = [1000, 2000, 5000, 10000, 25000]
val retryPeriods = config.get[List[Int]]("retry-periods")
```
<!-- .element: class="fragment" data-fragment-index="1" -->

Uses `Extractor[List[T]]` and `Extractor[RestConfig]`
<!-- .element: class="fragment" data-fragment-index="2" -->

```scala 3
/* services = [{
 *   host = "foo.net"
 *   port = 1234
 * }, {
 *   host = "bar.org"
 *   use-https = true
 * }]
 */
val services = config.get[List[RestConfig]]("services")
```
<!-- .element: class="fragment" data-fragment-index="2" -->

---
### Supporting additional Collections: Set

```scala 3
  given setExtractor[T](
      using ex: Extractor[List[T]]): Extractor[Set[T]] =
    (config, path) => ex.extract(config, path).toSet
```
<!-- .element: class="fragment" data-fragment-index="1" -->

Note: This is flexible

---

### Another Example: Maps

```
rest-services {
  service-a {
    host = "somerestapi.com"
    port = 9000
  }
  service-b {
    host = "anotherrestapi.com"
    port = 7777
  }
}
```

Desired semantics
```scala 3
val configMap = config.get[Map[String, RestConfig]](path)
```
---
### Map Extractor Implementation

```scala 3
given mapExtractor[T](
     using ex: Extractor[T]): Extractor[Map[String, T]] =
  (config, path) => {
    val c = config.getConfig(path)
    val keys = c.entrySet().asScala.map(_.getKey)
    keys.map(k => k -> ex.extract(c, k)).toMap
  }
```

Note: An object is basically just a key-value set, so we can map over keys

---

### Thanks
* Justin du Coeur - For convincing me that I had something worthwhile to talk about
* Mark Canlas - For helping me refine my final talk

---

### Additional Resources
* [ImplicitDesignPatternsInScala](https://www.lihaoyi.com/post/ImplicitDesignPatternsinScala.html) - A great article on implicits including Typeclasses
* https://github.com/kapunga/config-talk - Repo for this talk with working code examples
* Feel free to stop my my talk channel, #talk-practical-typeclass-usage for further questions.
* #scala-contributors or https://contributors.scala-lang.org/ to get started asking questions, seeing
how the sausage is made, and maybe start contributing.

Note: First article is Scala 2, but the principles are still very helpful. For getting involved in Typelevel,
stick around for the Typelevel roundtable later today.

---

### Questions?
#talk-practical-typeclass-usage in the Discord for more questions.
