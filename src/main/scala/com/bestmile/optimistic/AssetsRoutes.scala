package com.bestmile.optimistic

import akka.http.scaladsl.model.StatusCodes
import akka.http.scaladsl.server.Directives._

object AssetsRoutes {

  val routes =
    pathSingleSlash {
      redirect(uri = "/assets/index.html", StatusCodes.PermanentRedirect)
    } ~
    pathPrefix("assets" / Remaining) { file =>
      encodeResponse {
        getFromResource("assets/" + file)
      }
    }

}
