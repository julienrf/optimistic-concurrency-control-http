package com.bestmile.optimistic

import akka.actor.typed.{ActorSystem, SpawnProtocol}
import akka.actor.typed.scaladsl.adapter.TypedActorSystemOps
import akka.http.scaladsl.Http
import akka.http.scaladsl.model.StatusCodes
import endpoints4s.akkahttp.client

import scala.concurrent.Await
import scala.concurrent.duration.{Duration, DurationInt}

class ResourcesSuite extends munit.FunSuite {

  val interface = "0.0.0.0"
  val port = 8008
  val actorSystem: ActorSystem[SpawnProtocol.Command] = ActorSystem(SpawnProtocol(), "test-actor-system")
  import actorSystem.executionContext
  implicit val classicActorSystem: akka.actor.ActorSystem = actorSystem.toClassic

  val resourcesClient = {
    val settings =
      client.EndpointsSettings(client.AkkaHttpRequestExecutor.cachedHostConnectionPool(interface, port))
    new client.Endpoints(settings) with client.JsonEntitiesFromSchemas with ResourcesApi {
      def PreconditionFailed = StatusCodes.PreconditionFailed
    }
  }

  override def afterAll(): Unit = {
    actorSystem.terminate()
    Await.ready(actorSystem.whenTerminated, Duration.Inf)
    super.afterAll()
  }

  val withServer: FunFixture[Http.ServerBinding] = FunFixture.async(
    _ => HttpServer.bind(interface, port, actorSystem)(actorSystem),
    binding => binding.terminate(10.seconds).map(_ => ())
  )

  withServer.test("update a resource") { _ =>
    val newResource = Resource("new-value")
    for {
      // Read the current value of the resource
      resourceV1 <- resourcesClient.read()
      // Make sure it was different from what we will set it to
      _ = assert(resourceV1.value != newResource)
      // Try to update the resource with a new value
      maybeResourceV2 <- resourcesClient.update(newResource, resourceV1.version)
      // Check that the update was accepted
      _ = assert(maybeResourceV2.exists { resourceV2 =>
        resourceV2.value == newResource &&
          resourceV2.version != resourceV1.version
      })
      // Read the resource again
      resourceV2 <- resourcesClient.read()
      // Check that it still has the same value and version as what was returned by the previous update
      _ = assert(maybeResourceV2.contains(resourceV2))
    } yield ()
  }

  withServer.test("unable to update a resource without first reading its current version") { _ =>
    for {
      maybeResourceV1 <- resourcesClient.update(Resource("new-value"), Version("dummy-version"))
      _ = assert(maybeResourceV1.isEmpty)
    } yield ()
  }

  withServer.test("reject possibly conflicting updates") { _ =>
    for {
      resourceV1 <- resourcesClient.read()
      maybeResourceV2 <- resourcesClient.update(Resource("new-value"), resourceV1.version)
      _ = assert(maybeResourceV2.isDefined) // First updated succeeded
      maybeResourceV3 <- resourcesClient.update(Resource("newer-value"), resourceV1.version)
      _ = assert(maybeResourceV3.isEmpty) // Second update was rejected
      resourceV2 <- resourcesClient.read()
      _ = assert(maybeResourceV2.contains(resourceV2))
    } yield ()
  }

}
