### Practical Typeclass Usage
#### Improving Java Library Ergonomics

Paul (Thor) Thordarson

---

### Assumptions

* You understand how implicits work.

Note: speaker notes FTW!
Note: What are we going to do with all these speaker notes?

---

### What's a Typeclass?

---

### Where are typeclasses used?

---

### Our Java Config Library

_What We Have - Java API_
<!-- .element: class="fragment" data-fragment-index="1" -->

```java
public interface Config {
    boolean getBoolean(String path);
    double getDouble(String path);
    List<String> getIntList(String path);
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
  given Extractor[Float] =
    (config, path) => config.getNumber(path).floatValue()
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
    val host = config.get[String]("host")
    val port = config.get[Int]("port")
    val useHttps = config.get[Boolean]("use-https")

    RestConfig(host, port, useHttps)
  }
}
```

---

### This Allows

```
rest-config {
  host = "somerestapi.com"
  port = 9000
  use-https = true
}
```

```scala 3
val rc: RestConfig = config.get[RestConfig]("rest-config")
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

### The Solution: Nesting Givens

The Option Typeclass
<!-- .element: class="fragment" data-fragment-index="1" -->

```scala 3
object Extractor {
  given optExtractor[T: Extractor]: Extractor[Option[T]] =
    (config, path) => {
      if (config.hasPath(path)) {
        val ex = implicitly[Extractor[T]]
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
  def get[T](path: String)(using ex: Extractor[T]): T =
    ex.extract(config, path)

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
