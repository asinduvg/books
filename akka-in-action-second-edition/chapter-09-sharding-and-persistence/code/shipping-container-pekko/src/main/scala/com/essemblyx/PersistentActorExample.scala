import org.apache.pekko.persistence.typed.PersistenceId
import org.apache.pekko.persistence.typed.scaladsl.{EventSourcedBehavior, Effect}

object PersistentActorExample {

  def apply[Command, Event, State](
      persistenceId: PersistenceId, // must be unique
      state: State,
      commandHandler: (State, Command) => Effect[Event, State],
      eventHandler: (State, Event) => State
  ): EventSourcedBehavior[Command, Event, State] = ???

}


/**
    - persistenceId must be unique
      - cluster, we can have actors with sameId in differnet nodes
      - persistence doesn't have a mechanism to check uniqueness of a single node
        - but, shading has
        - so, we can create a sharded entity from EventSourcedBehavior
          - by adding EntityTypeKey
      - two different types of entities with sameID ?
        - sharding used to solve this problem
          - type key of the entity is appendedd to the ID of the persistent actor
            - this guarantees the uniqueness of the entity
      - to guarantee the uniqueness of a persistent actor in a cluster
        - combine with sharding
    - commandHandler
      - takes current state and message
        - outputs Event with new State
    - eventHandler
      - after the event is persisted by the commandHandler
        - eventHandler recieves event and update the actor's state
      - avoid side effects in eventHandler
        - will replay side effects as well on restart
  */