import org.scalajs.linker.interface.ModuleSplitStyle

import scala.sys.process._

val scala3Version = "3.4.2"

ThisBuild / scalaVersion := scala3Version

lazy val backend = crossProject(JVMPlatform)
  .withoutSuffixFor(JVMPlatform)
  .crossType(CrossType.Pure)
  .in(file("backend"))
  .settings(
    name := "hanko-exercise",
    version := "0.1.0-SNAPSHOT",
    libraryDependencies += "org.scalameta" %% "munit" % "1.0.0" % Test
  )

lazy val frontend = crossProject(JSPlatform)
  .withoutSuffixFor(JSPlatform)
  .crossType(CrossType.Pure)
  .in(file("frontend"))
  .enablePlugins(ScalablyTypedConverterExternalNpmPlugin)
  .jsSettings(
    scalaJSUseMainModuleInitializer := true,
    scalaJSLinkerConfig ~= {
      _.withModuleKind(ModuleKind.ESModule)
        .withModuleSplitStyle(
          ModuleSplitStyle.FewestModules
        ) // we have to choose this for Vite
    },
    externalNpm := {
      Process("pnpm i --frozen-lockfile", baseDirectory.value).!
      baseDirectory.value / ".."
    }
  )
  .settings(
    scalacOptions += "-noindent",
    libraryDependencies += "org.scala-js" %%% "scalajs-dom" % "2.4.0",
    libraryDependencies ++= Seq(
      "com.raquo" %%% "laminar" % "17.0.0",
      "com.raquo" %%% "waypoint" % "8.0.0"
    )
  )
