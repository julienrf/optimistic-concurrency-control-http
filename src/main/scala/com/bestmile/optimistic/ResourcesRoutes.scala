package com.bestmile.optimistic

import akka.http.scaladsl.server.Directives._enhanceRouteWithConcatenation
import akka.http.scaladsl.server.Route
import endpoints4s.akkahttp.server

class ResourcesRoutes(database: Database) extends ResourcesApi with server.Endpoints with server.JsonEntitiesFromSchemas {

  val routes: Route =
    read.implementedByAsync(_ => database.read()) ~
    update.implementedByAsync { case (resource, lastKnownVersion) => database.update(resource, lastKnownVersion) }

}
