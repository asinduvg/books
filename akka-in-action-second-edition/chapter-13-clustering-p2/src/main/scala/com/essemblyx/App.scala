package com.essemblyx

import org.apache.pekko.management.scaladsl.PekkoManagement
import org.apache.pekko.management.cluster.bootstrap.ClusterBootstrap
import org.apache.pekko.actor.typed.ActorSystem
import org.apache.pekko.actor.typed.scaladsl.Behaviors
import com.typesafe.config.ConfigFactory

object App {

  def main(args: Array[String]): Unit = {
    val i = args(0)

    val config = ConfigFactory
      .parseString(s"""
            pekko.remote.artery.canonical.hostname = "127.0.0.$i" 
            pekko.management.http.hostname = "127.0.0.$i"
        """)
      .withFallback(ConfigFactory.load())

    val system =
      ActorSystem[Nothing](Behaviors.empty, "testing-bootstrap", config)

    PekkoManagement(system).start()
    ClusterBootstrap(system).start()
  }
}
