package com.bestmile.optimistic

import akka.http.scaladsl.server.Route
import endpoints4s.akkahttp.server
import endpoints4s.openapi.model.OpenApi

object DocumentationRoutes extends server.Endpoints with server.JsonEntitiesFromEncodersAndDecoders {

  val getOpenApi = endpoint(
    get(path / "open-api.json"),
    ok(jsonResponse[OpenApi])
  ).implementedBy(_ => ResourcesDocs.api)

  val routes: Route = getOpenApi

}
