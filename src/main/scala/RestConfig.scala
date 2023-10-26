case class RestConfig(host: String, port: Int, useHttps: Boolean)

object RestConfig {
  given Extractor[RestConfig] = (config, path) => {
    val c = config.getConfig(path)
    val host = c.get[String]("host")
    val port = c.getOrElse[Int]("port", 8080)
    val useHttps = c.getOrElse[Boolean]("use-https", false)

    RestConfig(host, port, useHttps)
  }
}
