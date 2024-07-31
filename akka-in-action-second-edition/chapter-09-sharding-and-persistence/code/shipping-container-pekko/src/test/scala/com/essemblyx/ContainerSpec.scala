package com.essemblyx

import org.apache.pekko.cluster.sharding.typed.scaladsl.{ 
    ClusterSharding,
    Entity,
    EntityRef
}
import org.apache.pekko.cluster.sharding.typed.ShardingEnvelope
import org.apache.pekko.actor.testkit.typed.scaladsl.{
    LogCapturing,
    ScalaTestWithActorTestKit
}
import org.apache.pekko.actor.typed.ActorRef
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpecLike

// Sharding Cluster > Node > Shard Region > Shard > Entity

class ContainerSpec 
    extends ScalaTestWithActorTestKit
    with AnyWordSpecLike
    with Matchers
    with LogCapturing {

    "a sharded freight entity" should {
        "be able to add a cargo" in {
            // sharded cluster -> to send am message to entity
            val sharding = ClusterSharding(system)

            val entityDefinition = 
                Entity(Container.typeKey)(createBehavior = entityContext => Container(entityContext.entityId))
        
            // Sharding region created from the key and containerId
            val shardRegion: ActorRef[ShardingEnvelope[Container.Command]] =
                sharding.init(entityDefinition)

            val containerId = "id-1"
            val cargo = Container.Cargo("id-c", "sack", 3)

            // sends the message wrapped in an envelope
            shardRegion ! ShardingEnvelope(containerId, Container.AddCargo(cargo))

            val probe = createTestProbe[List[Container.Cargo]]()

            // gets entityRef by container id
            val container: EntityRef[Container.Command] =
                sharding.entityRefFor(Container.typeKey, containerId)

            // sends a message without sharding envelope, direct approach
            container ! Container.GetCargos(probe.ref)
            probe.expectMessage(List(cargo))

        }
    }
}
