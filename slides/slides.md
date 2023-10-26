### Practical Typeclass Usage
#### Improving Java Library Ergonomics

Paul (Thor) Thordarson

---

### Who Am I

---

### Target Audience

* You understand how implicits work.
* Perhaps seen typeclasses and have a vague understanding, but are unsure where/how you might go about making your own.

Note: The code in this talk is what finally made implicits click for me

---

### What's a Typeclass?

* Functional Programming equivalent of an interface for supporting _ad-hoc polymorphism_.
* Supports common behavior between unrelated types:
  * Comparison for equality or sorting.
  * Encoding/decoding instances into JSON.
  * Composing instances to create a new instance.
* Lets you extend a classes behaviour without extending the class itself.

Note: You don't need to control the source code of the class being extended. Useful for working with legacy or Java libraries.

---

### Where are typeclasses found?

* Standard Library - Traits like `Ordering` for sorting
* Spark - Traits like the _Dataset_ `Encoder`
* Typelevel ecosystem
  * Convenience traits like cats `Show`
  * Functional Programming traits like cats `Monoid`, `Applicative`, & `Functor`
  * Effect types like cats-effect `Async` & `Concurrent`

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

---
### Building our API

The Typeclass
<!-- .element: class="fragment" data-fragment-index="1" -->

```scala 3
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

---

### Custom Class

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

---

### This Allows

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

```scala 3
val serviceAConf: RestConfig =
  config.get[RestConfig]("service-a")
val serviceBConf: RestConfig =
  config.get[RestConfig]("service-b")
```

---

### What about `Option`?

What we have:
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

---

### The Solution: Givens Depending on other Givens

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

---

### Now we can do the following

```scala 3
  val host = config.get[String]("host")
  val port = config.get[Option[Int]]("port")
  val useHttps = config.get[Option[Boolean]]("use-https")
```

---

### Another Extension Method

```scala 3
extension (config: Config)
  ...
  
  def getOrElse[T: Extractor](path: String, default: T): T =
    get[Option[T]](path).getOrElse(default)
```

---

### Collecting errors

```scala 3
extension (config: Config)
  def get[T: Extractor](path: String): Either[Throwable, T] =
    try {
      val ex = implicitly[Extractor[T]]
      Right(ex.extract(config, path))
    } catch {
      case throwable: Throwable => Left(throwable)
    }

  def getOrElse[T: Extractor]
    (path: String, default: T): Either[Throwable, T] =
    
    get[Option[T]](path).map(_.getOrElse(default))
```
