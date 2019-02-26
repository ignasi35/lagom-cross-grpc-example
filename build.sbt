import akka.grpc.gen.scaladsl.play.PlayScalaServerCodeGenerator
import akka.grpc.gen.javadsl.play.PlayJavaClientCodeGenerator

organization in ThisBuild := "com.example"
version in ThisBuild := "1.0-SNAPSHOT"

// the Scala version that will be used for cross-compiled libraries
scalaVersion in ThisBuild := "2.12.4"

val macwire = "com.softwaremill.macwire" %% "macros" % "2.3.0" % "provided"
val scalaTest = "org.scalatest" %% "scalatest" % "3.0.4" % Test

lagomServiceEnableSsl in ThisBuild := true
val `hello-impl-HTTPS-port` = 11000


// ALL SETTINGS HERE ARE TEMPORARY WORKAROUNDS FOR KNOWN ISSUES OR WIP
def workaroundSettings: Seq[sbt.Setting[_]] = Seq(
  // Lagom still can't register a service under the gRPC name so we hard-code t
  // he port and the use the value to add the entry on the Service Registry
  lagomServiceHttpsPort := `hello-impl-HTTPS-port`
)

lazy val `lagom-cross-grpc-example` = (project in file("."))
  .aggregate(`hello-api`, `hello-api-grpc`, `hello-impl`, `hello-proxy-api`, `hello-proxy-impl`)

lazy val `hello-api` = (project in file("hello-api"))
  .settings(
    libraryDependencies += lagomScaladslApi
  )

lazy val `hello-api-grpc` = (project in file("hello-api-grpc"))
  .enablePlugins(AkkaGrpcPlugin) // enables source generation for gRPC
  .settings(
    akkaGrpcGeneratedLanguages := Seq(AkkaGrpc.Scala),
    akkaGrpcGeneratedSources :=
      Seq(
        AkkaGrpc.Server,
        AkkaGrpc.Client // the client is only used in tests. See https://github.com/akka/akka-grpc/issues/410
      ),
    akkaGrpcExtraGenerators in Compile += PlayScalaServerCodeGenerator,
  )
  .settings(
    libraryDependencies += lagomScaladslApi
  )

lazy val `hello-impl` = (project in file("hello-impl"))
  .enablePlugins(LagomScala)
  .enablePlugins(PlayAkkaHttp2Support) // enables serving HTTP/2 and gRPC
  .settings(
    workaroundSettings:_*
  ).settings(
    libraryDependencies ++= Seq(
      lagomScaladslTestKit,
      macwire,
      scalaTest
    )
  ).settings(lagomForkedTestSettings: _*)
  .dependsOn(
    `hello-api`,
    `hello-api-grpc`
  )


lazy val `hello-proxy-api` = (project in file("hello-proxy-api"))
  .settings(
    libraryDependencies +=lagomJavadslApi
  )

lazy val `hello-proxy-impl` = (project in file("hello-proxy-impl"))
  .enablePlugins(LagomJava)
  .enablePlugins(AkkaGrpcPlugin) // enables source generation for gRPC
  .settings(
    akkaGrpcGeneratedLanguages := Seq(AkkaGrpc.Java),
    akkaGrpcExtraGenerators += PlayJavaClientCodeGenerator,
  ).settings(
    libraryDependencies ++= Seq(
      organization.value %% "hello-api-grpc" % version.value % "protobuf"
    )
  )
  .dependsOn(
    `hello-proxy-api`
  )


// This sample application doesn't need either Kafka or Cassandra so we disable them
// to make the devMode startup faster.
lagomCassandraEnabled in ThisBuild := false
lagomKafkaEnabled in ThisBuild := false


// This adds an entry on the LagomDevMode Service Registry. With this information on
// the Service Registry a client using Service Discovery to Lookup("helloworld.GreeterService")
// will get "https://localhost:11000" and then be able to send a request.
// See declaration and usages of `hello-impl-HTTPS-port`.
lagomUnmanagedServices in ThisBuild := Map("helloworld.GreeterService" -> s"https://localhost:${`hello-impl-HTTPS-port`}")

