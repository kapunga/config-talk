import com.typesafe.config.ConfigFactory

@main def hello: Unit =
  val config = ConfigFactory.load()
  val foo = config.get[Option[String]]("foo")
  val myMap = config.get[Map[String, Int]]("my-map")
  val service = config.get[RestConfig]("service")
  val services = config.get[List[RestConfig]]("services")

  println(s"Config foo: $foo")
  println(s"Config my-map: $myMap")
  println(s"Config service: $service")
  println(s"Config services: $services")
