package eu.inn.hyperbus.transport.api

case class TransportConfiguration(clientRoutes: Seq[TransportRoute[ClientTransport]],
                                  serverRoutes: Seq[TransportRoute[ServerTransport]])
