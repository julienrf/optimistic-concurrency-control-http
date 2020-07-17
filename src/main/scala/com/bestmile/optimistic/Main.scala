package com.bestmile.optimistic

import akka.actor.typed.{ActorSystem, SpawnProtocol}
import org.slf4j.LoggerFactory

import scala.util.{Failure, Success}
import scala.concurrent.duration.DurationInt

object Main {

  val logger = LoggerFactory.getLogger(getClass)

  def main(args: Array[String]): Unit = {
    val actorSystem: ActorSystem[SpawnProtocol.Command] = ActorSystem(SpawnProtocol(), "actor-system")
    import actorSystem.executionContext

    val interface = "0.0.0.0"
    val port = 8000

    HttpServer.bind(interface, port, actorSystem)(actorSystem).andThen {
      case Failure(exception) =>
        logger.error(s"Unable to start the server: ${exception.getMessage}", exception)
        System.exit(1)
      case Success(binding) =>
        logger.info(s"Server started at http://$interface:$port")
        sys.addShutdownHook {
          binding.terminate(5.seconds).andThen {
            case Failure(exception) =>
              logger.error(s"Something went wrong when stopping the server: ${exception.getMessage}", exception)
              System.exit(1)
            case Success(_) =>
              logger.info("Server stopped")
          }
        }
    }
  }

}
