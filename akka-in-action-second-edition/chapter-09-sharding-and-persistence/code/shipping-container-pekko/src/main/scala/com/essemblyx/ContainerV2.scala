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
    command match
      case AddCargo(cargo) =>
        Effect.persist(CargoAdded(containerId, cargo))
      case GetCargos(replyTo) =>
        Effect.none.thenRun(state => replyTo ! state.cargos)

  def eventHandler(state: State, event: Event): State =
    event match
      case CargoAdded(containerId, cargo) =>
        state.copy(cargos = cargo +: state.cargos)

  def ready(cargos: List[Cargo]): Behavior[Command] =
    Behaviors.receiveMessage[Command] {
      case AddCargo(cargo) =>
        ready(cargo +: cargos)
      case GetCargos(replyTo) =>
        replyTo ! cargos
        Behaviors.same
    }

}

/** Why sharding is for: \- distributing locations of actors \- are called Entities when sharding \-
  * each entity is unique \- at most one in the cluster at any given time (entity) \- entities are
  * automatically relocated \- when new nodes are added \- or leaves the cluster \- because of
  * sharing resources (memory required is distributed among nodes) \- helpful in case of failures \-
  * entities are protected when node fails by moving them to another healthy member of the cluster
  * \- isolation by node also protects entities that live in healthy nodes from failures of other
  * nodes \- when moved, it loses it's state (as entity is restarted) \- to preserve, you need
  * persistence \- to become an entity, actor needs a key and ID \- key type: EntityTypeKey \-
  * refers to the type of the actor \- combination must be unique in the cluster \- allowing two
  * different types with same id without collisions \- creation and maintainance are overseen by
  * sharding module \- shards \- logical containers which entities are grouped \- contain
  * distributed entities \- number of shards is determined by the configuration \- content is
  * determined by the sharding function \- node in sharded cluster can create shard regions (SRs) \-
  * that contain shards \- responsible for routing messages to entities \- if do not know, ask SC \-
  * routing messages, done in two ways \- send the message to the SR of the same node \- if the
  * entitiy is in different node (or SR) \- forwards to that SR where entity is located \- send the
  * message directly to entity (obtaining reference) \- tell(!) \- shard coordinator (SC) \- manages
  * which shards belong to which SR \- manages how to find entities (facilitating access to each
  * entity) \- only one in the cluster \- monitor the state of the shards \- to ensure that shards
  * are moved from congested or unhealthy region to healthy one \- means, unreachable node must be
  * removed from the cluster before shard and its entities are moved \- otherwise, previous
  * unreachable node could rejoin the cluster after rebalancing \- hence, there would be duplicates
  * \- this is bad, state inconsistencies \- SBR (Split Brain Resolver manage unreachable nodes) \-
  * rebalancing (not a failure) \- messages are kept in a buffer \- until the displaced entity is
  * restored \- lose a node \- lose messages \- before all regions notice the loss and start
  * buffering \- messages are not buffered until SR marked unavailable
  *
  * Shipping Example \- shipping company \- need: move containers \- choose: origin & destination,
  * type of cargo, type of container, etc... \- distribute the app \- load evenly \- API \-
  * Container \- Cargo(id, kind, size) \- AddCargo(cargo) \- GetCargos(replyTo:
  * ActorRef[List[Cargo]]) \- to make this actor, an entity \- key: "container-type-key" \- id:
  * containerId from the constructor \- to send msgs to that container entity \- must have at least
  * one shard cluster \- sharding = ClusterSharding(system) \- and one SR init from it \-
  * sharding.init( Entity("container-type-key", entityContext => Container(entityContext.entityId))
  * ) \- creates actorRef(EntityRef) for us \- shardRegion ! ShardingEnvelope("id-1",
  * Container.AddCargo(cargo)) \- EntityContext \- not similar to ActorContext \- no watch, logging,
  * spawning, scheduling \- the system handles the lifecycle and rebalancing, not context \-
  */
