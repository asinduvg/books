package com.essemblyx

import org.apache.pekko.actor.typed.{ActorRef, Behavior}
import org.apache.pekko.actor.typed.scaladsl.{Behaviors}
import org.apache.pekko.cluster.sharding.typed.scaladsl.EntityTypeKey
import org.apache.pekko.persistence.typed.scaladsl.{EventSourcedBehavior, Effect}
import org.apache.pekko.persistence.typed.PersistenceId
import org.apache.pekko.persistence.typed.scaladsl.RetentionCriteria
import org.apache.pekko.actor.typed.SupervisorStrategy

import scala.concurrent.duration.DurationInt

object SPContainer {

  final case class Cargo(id: String, kind: String, size: Int)

  val typeKey = EntityTypeKey[SPContainer.Command]("spcontainer-type-key")

  sealed trait Command
  final case class AddCargo(cargo: Cargo)                    extends Command with CborSerializable
  final case class GetCargos(replyTo: ActorRef[List[Cargo]]) extends Command with CborSerializable

  sealed trait Event
  final case class CargoAdded(containerId: String, cargo: Cargo) extends Event with CborSerializable
  final case class State(cargos: List[Cargo] = Nil)

  def apply(containerId: String): Behavior[Command] =
    EventSourcedBehavior[Command, Event, State](
      PersistenceId(typeKey.name, containerId),
      State(),
      commandHandler = (state, command) => commandHandler(containerId, state, command),
      eventHandler
    ).onPersistFailure(
      SupervisorStrategy.restartWithBackoff(
        minBackoff = 10.seconds,
        maxBackoff = 60.seconds,
        randomFactor = 0.1
      )
    )
    // .receiveSignal {
    //   case (state, RecoveryCompleted)       => ??? // entire journal is read without trouble
    //   case (state, RecoveryFailed(failure)) => ???
    // }
    // .snapshotWhen {
    //   case (state, QuarterReached(_), sequenceNumber) => true
    //   case (state, event, sequenceNumber)             => false
    // }
    .withRetention(
      RetentionCriteria.snapshotEvery(numberOfEvents = 100, keepNSnapshots = 2)
    ).withTagger {
      case _ => Set("container-tag-" + containerId.toInt % 3)
    }

  def commandHandler(
      containerId: String,
      state: State,
      command: Command
  ): Effect[Event, State] =
    command match {
      case AddCargo(cargo) =>
        Effect.persist(CargoAdded(containerId, cargo))
      case GetCargos(replyTo) =>
        Effect.none.thenRun(state => replyTo ! state.cargos)
    }

  def eventHandler(state: State, event: Event): State =
    event match {
      case CargoAdded(containerId, cargo) =>
        state.copy(cargos = cargo +: state.cargos)
    }

  def ready(cargos: List[Cargo]): Behavior[Command] =
    Behaviors.receiveMessage[Command] {
      case AddCargo(cargo) =>
        ready(cargo +: cargos)
      case GetCargos(replyTo) =>
        replyTo ! cargos
        Behaviors.same
    }

}
