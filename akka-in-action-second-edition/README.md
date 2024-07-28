** Clustering **

```
pekko {
  actor {
    provider = "cluster"
  } 
  remote {
    artery {
      transport = tcp
      canonical.hostname = 127.0.0.1
      canonical.port = ${PORT}
    }
  }
  cluster {
    seed-nodes = [
     "pekko://words@127.0.0.1:2551",
     "pekko://words@127.0.0.1:2552",
     "pekko://words@127.0.0.1:2553"
    ]
  }
  downing-provider-class = "org.apache.pekko.cluster.sbr.SplitBrainResolverProvider"
  management{
    http.port = 8558
    http.hostname = "<hostname>"
  }
}
```
{PORT} substitution doesn't work if we give it like following.
```
sbt -DPORT=2551 run
```
In that case you can export the PORT variable and run the sbt.
```
export PORT=2551
sbt run
```

You can also set variables like following.
```
sbt -Dpekko.management.http.port=8558 run
```
Above is useful when you want to run multiple management nodes on the same machine.

