package xt

import java.util.Properties
import java.net.URL
import java.io.FileInputStream

object Config {
  private val properties = {
    val stream = getClass.getClassLoader.getResourceAsStream("xitrum.properties")
    val ret = new Properties
    ret.load(stream)
    ret
  }

  val isProductionMode = (System.getProperty("xitrum.mode") == "production")

  val httpPort = properties.getProperty("http_port", "8080").toInt

  val maxContentLength = properties.getProperty("max_content_length", "1048576").toInt  // default: 10MB

  val sessionIdName = properties.getProperty("session_id_name", "JSESSIONID")
  val sessionsEhcacheName = properties.getProperty("sessions_ehcache_name", "XitrumSessions")

  val filterParams = properties.getProperty("filter_params", "password").split(", ")

  val filesEhcacheName = properties.getProperty("files_ehcache_name", "XitrumFiles")
  val filesMaxSize     = properties.getProperty("files_max_size", "102400").toInt
}
