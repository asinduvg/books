package com.essemblyx

import org.apache.pekko.actor.typed.{ActorRef, Behavior}
import org.apache.pekko.actor.typed.scaladsl.{Behaviors}
import org.apache.pekko.persistence.typed.scaladsl.{EventSourcedBehavior, Effect}
import org.apache.pekko.persistence.typed.PersistenceId

object ContainerV2 {

  final case class Cargo(id: String, kind: String, size: Int)

  sealed trait Command
  final case class AddCargo(cargo: Cargo)                    extends Command with CborSerializable
  final case class GetCargos(replyTo: ActorRef[List[Cargo]]) extends Command with CborSerializable

  sealed trait Event
  final case class CargoAdded(containerId: String, cargo: Cargo) extends Event with CborSerializable
  final case class State(cargos: List[Cargo] = Nil)

  def apply(containerId: String): Behavior[Command] =
    EventSourcedBehavior[Command, Event, State](
      PersistenceId.ofUniqueId(containerId),
      State(),
      commandHandler = (state, command) => commandHandler(containerId, state, command),
      eventHandler
    )

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
