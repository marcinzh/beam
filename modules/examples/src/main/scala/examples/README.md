## Examples

Runnable with `scala-cli` or from `sbt`:

```sh
sbt examples/run
```

> [!IMPORTANT]
> Turbolift requires **Java 11** or newer.

Source file | Description
:---|:---
[ex00_basic](ex00_basic.scala) | Basic stream from a collection.
[ex01_sourceEffect](ex01_stutterFibo.scala) | Uses `Source` effect to create a stream
[ex02_pipeEffect](ex02_pipeEffect.scala) | Uses `Pipe` effect to transform a stream
[ex03_resource](ex03_resource.scala) | Streams created with simulated resource
