package com.bestmile.optimistic

import endpoints4s.openapi
import endpoints4s.openapi.model.{Info, OpenApi}

object ResourcesDocs extends ResourcesApi with openapi.Endpoints with openapi.JsonEntitiesFromSchemas {

  val api: OpenApi = openApi(
    Info("Optimistic Concurrency Control", "1.0.0")
  )(read, update)

}
