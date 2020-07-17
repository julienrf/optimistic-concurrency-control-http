package com.bestmile.optimistic

import akka.actor.typed._
import akka.actor.typed.scaladsl.Behaviors
import akka.util.Timeout

import scala.concurrent.Future
import scala.concurrent.duration.DurationInt

/**
 * A simplified database performing optimistic concurrency control on a single resource.
 */
trait Database {
  /**
   * @return The current state of the resource, along with its version number
   */
  def read(): Future[Versioned[Resource]]
  /**
   * Try to update the state of the resource.
   * @param newValue         New resource value
   * @param lastKnownVersion Last known version of this resource
   * @return Some(newVersionedResource), if the update was successful, otherwise None
   */
  def update(newValue: Resource, lastKnownVersion: Version): Future[Option[Versioned[Resource]]]
}

object Database {

  /**
   * An in-memory implementation of the `Database` API, for demonstration purpose.
   *
   * In practice, something like [[https://github.com/gonmarques/slick-repo#optimistic-locking-versioning slick-repo]]
   * could be used.
   */
  class InMemoryDatabase private (ref: ActorRef[InMemoryDatabase.Command])(implicit actorSystem: ActorSystem[_]) extends Database {
    import akka.actor.typed.scaladsl.AskPattern._
    implicit val timeout: Timeout = 10.seconds

    def read(): Future[Versioned[Resource]] =
      ref.ask(InMemoryDatabase.Read(_))

    def update(newValue: Resource, lastKnownVersion: Version): Future[Option[Versioned[Resource]]] =
      ref.ask(InMemoryDatabase.Update(newValue, lastKnownVersion, _))
  }

  object InMemoryDatabase {
    private sealed trait Command
    private case class Read(replyTo: ActorRef[Versioned[Resource]]) extends Command
    private case class Update(newValue: Resource, lastKnownVersion: Version, replyTo: ActorRef[Option[Versioned[Resource]]]) extends Command

    /** Creates an `InMemoryDatabase` */
    def apply(spawnRef: ActorRef[SpawnProtocol.Command])(implicit actorSystem: ActorSystem[_]): Future[InMemoryDatabase] = {
      import actorSystem.executionContext
      import akka.actor.typed.scaladsl.AskPattern._
      implicit val timeout: Timeout = 10.seconds

      def behavior(current: Versioned[Resource]): Behavior[Command] =
        Behaviors.receiveMessage {
          case Read(replyTo) =>
            // Send the current version of the resource
            replyTo ! current
            Behaviors.same
          case Update(newValue, lastKnownVersion, replyTo) =>
            // Compute the new state, if the current resource version matches the `lastKnownVersion`
            val maybeUpdated =
              if (lastKnownVersion == current.version) Some(Versioned(newValue, Version.random()))
              else None
            replyTo ! maybeUpdated
            behavior(maybeUpdated.getOrElse(current))
        }

      val initState = Versioned(Resource(""), Version.random())
      spawnRef
        .ask[ActorRef[Command]](SpawnProtocol.Spawn(behavior(initState), "database", Props.empty, _))
        .map(ref => new InMemoryDatabase(ref))
    }
  }

}


