import com.typesafe.config.{Config, ConfigFactory, ConfigValueFactory}

import scala.jdk.CollectionConverters.given

@scala.annotation.implicitNotFound("Missing given Extractor for type ${T}, make sure you define one!")
trait Extractor[T] {
  def extract(config: Config, path: String): T
}

object Extractor {
  given Extractor[String] with
    def extract(config: Config, path: String): String = config.getString(path)

  given Extractor[Int] = (config, path) => config.getInt(path)
  given Extractor[Long] = (config, path) => config.getLong(path)
  given Extractor[Float] = (config, path) => config.getNumber(path).floatValue()
  given Extractor[Double] = (config, path) => config.getDouble(path)
  given Extractor[Boolean] = (config, path) => config.getBoolean(path)

  given optionExtractor[T](using ex: Extractor[T]): Extractor[Option[T]] = (config, path) => {
    if (config.hasPath(path))
      Some(ex.extract(config, path))
    else
      None
  }

  given stringListExtractor: Extractor[List[String]] =
    (config, path) => config.getStringList(path).asScala.toList
  given intListExtractor: Extractor[List[Int]] =
    (config, path) => config.getIntList(path).asScala.toList.map(_.toInt)
  given longListExtractor: Extractor[List[Long]] =
    (config, path) => config.getLongList(path).asScala.toList.map(_.toLong)
  given floatListExtractor: Extractor[List[Float]] =
    (config, path) => config.getNumberList(path).asScala.toList.map(_.floatValue().toFloat)
  given doubleListExtractor: Extractor[List[Double]] =
    (config, path) => config.getDoubleList(path).asScala.toList.map(_.toDouble)
  given booleanListExtractor: Extractor[List[Boolean]] =

    (config, path) => config.getBooleanList(path).asScala.toList.map(_.booleanValue)

  given listExtractor[T](using ex: Extractor[T]): Extractor[List[T]] =
    (config, path) => {
      def wrap(conf: Config): Config =
        ConfigValueFactory.fromMap(Map("key" -> conf.root().unwrapped()).asJava).toConfig
      config.getConfigList(path).asScala.toList.map(c => ex.extract(wrap(c), "key"))
    }
  given setExtractor[T](using ex: Extractor[List[T]]): Extractor[Set[T]] =
    (config, path) => ex.extract(config, path).toSet

  given mapExtractor[T](using ex: Extractor[T]): Extractor[Map[String, T]] =
    (config, path) => {
      val c = config.getConfig(path)
      val keys = c.entrySet().asScala.map(_.getKey)
      keys.map(k => k -> ex.extract(c, k)).toMap
    }
}

extension (config: Config)
  def get[T](path: String)(using ex: Extractor[T]): T =
    ex.extract(config, path)

  def getOrElse[T](path: String, default: T)(using ex: Extractor[Option[T]]): T =
    ex.extract(config, path).getOrElse(default)
