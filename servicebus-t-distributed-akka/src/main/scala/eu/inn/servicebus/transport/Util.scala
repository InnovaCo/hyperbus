package eu.inn.servicebus.transport

import akka.actor.ActorSystem
import scala.concurrent.duration
import duration._

private [transport] object Util {
  val defaultTimeout = 20.second
  val defaultEncoding = "UTF-8"

  // empty group doesn't work, so we need to have some default string
  def getUniqGroupName(groupName: Option[String]): Option[String] = {
    val defaultGroupName = "-default-"
    groupName.map{ s ⇒
      if (s.startsWith(defaultGroupName))
        defaultGroupName + s
      else
        s
    } orElse {
      Some(defaultGroupName)
    }
  }
}
