package eu.inn.hyperbus.model

import java.io.{ByteArrayInputStream, InputStream, OutputStream}

import com.fasterxml.jackson.core.JsonParser
import eu.inn.binders.dynamic.Value
import eu.inn.hyperbus.serialization.{MessageDeserializer, RequestHeader}
import eu.inn.hyperbus.transport.api.uri.Uri

trait DynamicBody extends Body with Links {
  def content: Value

  lazy val links: LinksMap.LinksMapType = content.__links[Option[LinksMap.LinksMapType]].getOrElse(Map.empty)

  def serialize(outputStream: OutputStream): Unit = {
    import eu.inn.binders._
    eu.inn.binders.json.SerializerFactory.findFactory().withStreamGenerator(outputStream) { serializer =>
      serializer.bind[Value](content)
    }
  }

  def copy(
            contentType: Option[String] = this.contentType,
            content: Value = this.content
          ): DynamicBody = {
    DynamicBody(contentType, content)
  }
}

object DynamicBody {
  def apply(contentType: Option[String], content: Value): DynamicBody = DynamicBodyContainer(contentType, content)

  def apply(content: Value): DynamicBody = DynamicBodyContainer(None, content)

  def apply(contentType: Option[String], jsonParser: com.fasterxml.jackson.core.JsonParser): DynamicBody = {
    import eu.inn.binders.json._
    SerializerFactory.findFactory().withJsonParser(jsonParser) { deserializer =>
      apply(contentType, deserializer.unbind[Value])
    }
  }

  def unapply(dynamicBody: DynamicBody) = Some((dynamicBody.contentType, dynamicBody.content))
}

private[model] case class DynamicBodyContainer(contentType: Option[String], content: Value) extends DynamicBody

case class DynamicRequest(uri: Uri,
                          body: DynamicBody,
                          headers: Headers) extends Request[DynamicBody]

object DynamicRequest {
  def apply(requestHeader: RequestHeader, jsonParser: JsonParser): DynamicRequest = {
    val b = DynamicBody(requestHeader.contentType, jsonParser)
    apply(requestHeader, b)
  }

  def apply(message: String, encoding: String = "UTF-8"): DynamicRequest = {
    apply(new ByteArrayInputStream(message.getBytes(encoding)))
  }

  def apply(inputStream: InputStream): DynamicRequest = {
    MessageDeserializer.deserializeRequestWith(inputStream) { (requestHeader, jsonParser) ⇒
      DynamicRequest(requestHeader, jsonParser)
    }
  }

  def apply(requestHeader: RequestHeader, body: DynamicBody): DynamicRequest = {
    DynamicRequest(requestHeader.uri, body, Headers.plain(requestHeader.headers))
  }

  def apply(uri: Uri, method: String, body: DynamicBody, headers: Headers): DynamicRequest = {
    DynamicRequest(uri, body, new HeadersBuilder(headers)
      .withMethod(method)
      .withContentType(body.contentType)
      .result())
  }

  def apply(uri: Uri, method: String, body: DynamicBody)
           (implicit contextFactory: MessagingContextFactory): DynamicRequest = {
    apply(uri, method, body, Headers())
  }
}
