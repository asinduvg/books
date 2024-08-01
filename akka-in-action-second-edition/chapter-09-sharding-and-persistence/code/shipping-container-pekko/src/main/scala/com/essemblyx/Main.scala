import org.apache.pekko.cluster.typed.{Cluster}
import org.apache.pekko.cluster.sharding.typed.scaladsl.{ClusterSharding, Entity, EntityTypeKey}
import org.apache.pekko.cluster.sharding.typed.ShardingEnvelope
import org.apache.pekko.actor.typed.{ActorRef, ActorSystem, Behavior}
import org.apache.pekko.actor.typed.scaladsl.Behaviors

import org.slf4j.LoggerFactory
import scala.util.control.NonFatal
import scala.io.StdIn
import scala.annotation.tailrec
import com.essemblyx.SPContainer

import com.essemblyx.CommandLine.Command

object Main {

  val logger = LoggerFactory.getLogger(Main.getClass)

  def main(args: Array[String]): Unit = {
    val system = ActorSystem[Nothing](Behaviors.empty, "containers")
    try {
      val shardRegion = init(system)
      commandLoop(system, shardRegion)
    } catch {
      case NonFatal(ex) =>
        logger.error(s"terminating by NonFatal Exception", ex)
        system.terminate()
    }
  }

  def init(system: ActorSystem[_]): ActorRef[ShardingEnvelope[SPContainer.Command]] = {
    val sharding = ClusterSharding(system)
    val entityDefinition =
      Entity(SPContainer.typeKey)(entityContext => SPContainer(entityContext.entityId))

    val shardRegion: ActorRef[ShardingEnvelope[SPContainer.Command]] =
      sharding.init(entityDefinition)

    shardRegion
  }

  @tailrec
  private def commandLoop(
      system: ActorSystem[_],
      shardRegion: ActorRef[ShardingEnvelope[SPContainer.Command]]
  ): Unit = {
    print("please write: ")
    val commandString = StdIn.readLine()

    if (commandString == null)
      system.terminate
    else
      Command(commandString) match {
        case Command.AddCargo(
              containerId,
              cargoId,
              cargoKind,
              cargoSize
            ) =>
          shardRegion ! ShardingEnvelope(
            containerId,
            SPContainer.AddCargo(
              SPContainer.Cargo(cargoId, cargoKind, cargoSize)
            )
          )
          commandLoop(system, shardRegion)
        case Command.Unknown(command) =>
          logger.warn("Unknown command {}!", command)
          commandLoop(system, shardRegion)
        case Command.Quit =>
          logger.info("Terminating by user signal")
          system.terminate
          commandLoop(system, shardRegion)
      }
  }
}
