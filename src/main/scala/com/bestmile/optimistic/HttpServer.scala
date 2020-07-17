package com.bestmile.optimistic

import akka.actor.typed.{ActorRef, ActorSystem, SpawnProtocol}
import akka.actor.typed.scaladsl.adapter.TypedActorSystemOps
import akka.http.scaladsl.Http
import akka.http.scaladsl.server.Directives._enhanceRouteWithConcatenation

import scala.concurrent.Future

object HttpServer {
  def bind(
    interface: String,
    port: Int,
    spawnRef: ActorRef[SpawnProtocol.Command]
  )(
    actorSystem: ActorSystem[_]
  ): Future[Http.ServerBinding] = {
    implicit val classicActorSystem: akka.actor.ActorSystem = actorSystem.toClassic
    import actorSystem.executionContext
    Database.InMemoryDatabase(spawnRef)(actorSystem).flatMap { database =>
      val resourcesRoutes = new ResourcesRoutes(database)
      val routes = resourcesRoutes.routes ~ DocumentationRoutes.routes ~ AssetsRoutes.routes
      Http().bindAndHandle(routes, interface, port)
    }
  }
}
