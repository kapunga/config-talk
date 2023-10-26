import com.typesafe.config.ConfigFactory

@main def hello: Unit =
  val config = ConfigFactory.load()
  val foo = config.get[Option[String]]("foo")

  println(s"Config foo: $foo")
  println("Hello world!")
  println(msg)

def msg = "I was compiled by Scala 3. :)"
