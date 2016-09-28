package eu.inn.hyperbus.transport.httpclient

import org.asynchttpclient.proxy.ProxyServer
import org.asynchttpclient.{AsyncHttpClient, DefaultAsyncHttpClient, DefaultAsyncHttpClientConfig, Realm}

import scala.concurrent.duration.FiniteDuration

case class HttpClientProxyConfig(
                                host: String,
                                port: Int,
                                securePort: Option[Int],
                                excludeHosts: List[String],
                                userName: Option[String],
                                password: Option[String]
                                )

case class HttpClientConfig(
                             connectTimeout: Option[FiniteDuration],
                             readTimeout: Option[FiniteDuration],
                             acceptAnyCertificate: Option[Boolean],
                             proxy: Option[HttpClientProxyConfig]
                           ) {
  def createHttpClient(): AsyncHttpClient = {
    val cf = new DefaultAsyncHttpClientConfig.Builder()
    connectTimeout.foreach(t ⇒ cf.setConnectTimeout(t.toMillis.toInt))
    readTimeout.foreach(t ⇒ cf.setConnectTimeout(t.toMillis.toInt))
    acceptAnyCertificate.foreach(cf.setAcceptAnyCertificate)
    cf.setThreadPoolName("hyperbus-t-http-client-ning")
    proxy.foreach { p ⇒
      val pb = new ProxyServer.Builder(p.host, p.port)
      p.securePort.foreach(pb.setSecuredPort)
      p.userName.foreach{ u ⇒
        val rb = new Realm.Builder(u, p.password.getOrElse(""))
        pb.setRealm(rb.build())
      }
      cf.setProxyServer(pb)
    }
    new DefaultAsyncHttpClient(cf.build())
  }
}
