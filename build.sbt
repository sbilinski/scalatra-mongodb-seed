name := "scalatra-mongodb-seed"

organization := "com.mintbeans"

version := "0.1-SNAPSHOT"

scalaVersion := "2.11.7"

scalacOptions ++= Seq("-feature", "-unchecked", "-deprecation", "-encoding", "utf8")

mainClass := Some("com.mintbeans.geo.LocationProvider")

resolvers ++= Seq(
    "Sonatype Snapshots"  at "https://oss.sonatype.org/content/repositories/snapshots/",
    "Sonatype Releases"   at "http://oss.sonatype.org/content/repositories/releases",
    "Typesafe Repository" at "http://repo.typesafe.com/typesafe/releases/"
)

libraryDependencies ++= {
    val configVersion     = "1.3.0"
    val logbackVersion    = "1.1.3"
    val scalatraVersion   = "2.4.0"
    val jettyVersion      = "9.3.7.v20160115"
    val json4sVersion     = "3.3.0"
    val casbahVersion     = "2.7.1" //Salat should be upgraded/removed when switching to 3.x
    val salatVersion      = "1.9.9"
    val embedMongoVersion = "0.2.2"
    val macwireVersion    = "2.2.2"
    val scalaMockVersion  = "3.2.1"
    Seq(
        "com.typesafe"              %   "config"                      % configVersion,
        "org.scalatra"              %%  "scalatra"                    % scalatraVersion,
        "org.scalatra"              %%  "scalatra-json"               % scalatraVersion,
        "org.scalatra"              %%  "scalatra-swagger"            % scalatraVersion,
        "org.scalatra"              %%  "scalatra-scalatest"          % scalatraVersion % "test",
        "org.json4s"                %%  "json4s-jackson"              % json4sVersion,
        "org.eclipse.jetty"         %   "jetty-server"                % jettyVersion,
        "org.eclipse.jetty"         %   "jetty-webapp"                % jettyVersion,
        "org.mongodb"               %%  "casbah-core"                 % casbahVersion,
        "com.novus"                 %%  "salat"                       % salatVersion,
        "com.github.simplyscala"    %%  "scalatest-embedmongo"        % embedMongoVersion % "test",
        "com.softwaremill.macwire"  %%  "macros"                      % macwireVersion,
        "ch.qos.logback"            %   "logback-classic"             % logbackVersion,
        "org.scalamock"             %%  "scalamock-scalatest-support" % scalaMockVersion % "test"
    )
}

//
// Cached resolution (http://www.scala-sbt.org/0.13/docs/Cached-Resolution.html)
//
updateOptions := updateOptions.value.withCachedResolution(true)

//
// Scalariform
//
import scalariform.formatter.preferences._
import com.typesafe.sbt.SbtScalariform._

SbtScalariform.scalariformSettings

ScalariformKeys.preferences := ScalariformKeys.preferences.value
  .setPreference(AlignSingleLineCaseStatements, true)
  .setPreference(AlignSingleLineCaseStatements.MaxArrowIndent, 100)
  .setPreference(DoubleIndentClassDeclaration, true)
  .setPreference(DanglingCloseParenthesis, Preserve)

//
// Revolver
//
Revolver.settings:Seq[sbt.Setting[_]]

//
// Assembly
//
import AssemblyKeys._

assemblySettings

jarName in assembly := "location-provider.jar"

//
// Docker
//
import DockerKeys._
import sbtdocker.mutable.Dockerfile
import sbtdocker.ImageName

dockerSettings

docker <<= (docker dependsOn assembly)

dockerfile in docker := {
    val artifact = (outputPath in assembly).value
    val artifactTargetPath = s"/app/${artifact.name}"
    val configFile = baseDirectory.value / "src" / "main" / "resources" / "docker.conf"
    val configFileTargetPath = s"/app/application.conf"
    val logbackFile = baseDirectory.value / "src" / "main" / "resources" / "logback.xml"
    val logbackFileTargetPath = "/app/logback.xml"
    new Dockerfile {
        from("java:8")
        add(artifact, artifactTargetPath)
        add(configFile, configFileTargetPath)
        add(logbackFile, logbackFileTargetPath)
        env("MONGO_DB",   "test")
        env("MONGO_HOST", "localhost")
        env("MONGO_PORT", "27017")
        entryPoint("java",
                  s"-Dconfig.file=${configFileTargetPath}",
                  s"-Dlogback.configurationFile=${logbackFileTargetPath}",
                   "-jar", artifactTargetPath)
    }
}

imageName in docker := {
    ImageName(namespace = Some(organization.value),
              repository = name.value,
              tag = Some("v" + version.value))
}
