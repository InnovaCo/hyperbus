package eu.inn.hyperbus.model.annotations

import scala.annotation.{StaticAnnotation, compileTimeOnly}
import scala.language.experimental.macros
import scala.reflect.macros.blackbox.Context

// todo: JsonHalSerializerFactory and bindOptions as implicit arguments!

@compileTimeOnly("enable macro paradise to expand macro annotations")
class body(v: String) extends StaticAnnotation {
  def macroTransform(annottees: Any*): Any = macro BodyMacroImpl.body
}

private[annotations] object BodyMacroImpl {
  def body(c: Context)(annottees: c.Expr[Any]*): c.Expr[Any] = {
    val c0: c.type = c
    val bundle = new {
      val c: c0.type = c0
    } with BodyAnnotationMacroImpl
    bundle.run(annottees)
  }
}

private[annotations] trait BodyAnnotationMacroImpl extends AnnotationMacroImplBase {

  import c.universe._

  def updateClass(existingClass: ClassDef, clzCompanion: Option[ModuleDef] = None): c.Expr[Any] = {
    val contentType = c.prefix.tree match {
      case q"new body($contentType)" => c.Expr(contentType)
      case _ ⇒ c.abort(c.enclosingPosition, "Please provide arguments for @body annotation")
    }

    val q"case class $className(..$fields) extends ..$bases { ..$body }" = existingClass

    val fVal = fresh("f")
    val serializerVal = fresh("serializer")
    val deserializerVal = fresh("deserializer")
    val newClass = q"""
        @eu.inn.hyperbus.model.annotations.contentType($contentType) case class $className(..$fields) extends ..$bases {
          ..$body
          def contentType = Some($contentType)
          override def serialize(outputStream: java.io.OutputStream) = {
            import eu.inn.hyperbus.serialization.MessageSerializer.bindOptions
            implicit val $fVal = new eu.inn.hyperbus.serialization.JsonHalSerializerFactory[eu.inn.binders.naming.PlainConverter]
            eu.inn.binders.json.SerializerFactory.findFactory().withStreamGenerator(outputStream) { case $serializerVal =>
              $serializerVal.bind[$className](this)
            }
          }
        }
      """

    // check requestHeader
    val companionExtra = q"""
        def contentType = Some($contentType)
        def apply(contentType: Option[String], jsonParser : com.fasterxml.jackson.core.JsonParser): $className = {
          implicit val $fVal = new eu.inn.hyperbus.serialization.JsonHalSerializerFactory[eu.inn.binders.naming.PlainConverter]
          eu.inn.binders.json.SerializerFactory.findFactory().withJsonParser(jsonParser) { case $deserializerVal =>
            $deserializerVal.unbind[$className]
          }
        }
        """

    val newCompanion = clzCompanion map { existingCompanion =>
      val q"object $companion extends ..$bases { ..$body }" = existingCompanion
      q"""
          object $companion extends ..$bases {
            ..$body
            ..$companionExtra
          }
        """
    } getOrElse {
      q"""
        object ${className.toTermName} {
          ..$companionExtra
        }
      """
    }

    c.Expr(q"""
        $newClass
        $newCompanion
      """
    )
  }
}
