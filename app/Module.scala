/**
  * Copyright 2017 Interel
  *
  * Licensed under the Apache License, Version 2.0 (the "License");
  * you may not use this file except in compliance with the License.
  * You may obtain a copy of the License at
  *
  * http://www.apache.org/licenses/LICENSE-2.0
  *
  * Unless required by applicable law or agreed to in writing, software
  * distributed under the License is distributed on an "AS IS" BASIS,
  * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
  * See the License for the specific language governing permissions and
  * limitations under the License.
  */
import akka.actor.ActorSystem
import akka.stream.Materializer
import akka.util.Timeout
import com.google.inject.{AbstractModule, Provides, Singleton}
import core3.config.StaticConfig
import core3.core.{ComponentManager, ComponentManagerActor}
import core3.database.ContainerType
import core3.database.containers.{JSONContainerCompanion, JSONConverter, core}
import core3.http.filters.{CompressionFilter, MaintenanceModeFilter, MetricsFilter, TraceFilter}
import core3.http.requests.{WorkflowEngineConnection, local}
import core3.core.Component.ComponentDescriptor
import core3.core.cli.LocalConsole
import core3.database.dals.{Core, DatabaseAbstractionLayer}
import core3.database.dals.json.Redis
import core3.workflows.{WorkflowBase, definitions}
import net.codingwell.scalaguice.ScalaModule
import play.api.{Environment, Mode}
import play.api.libs.ws.WSClient

import scala.concurrent.ExecutionContext
import scala.concurrent.duration._

class Module extends AbstractModule with ScalaModule {

  override def configure() = {
    bind(classOf[ConsoleStart]).to(classOf[ConsoleStartImpl]).asEagerSingleton()
  }

  @Provides
  def provideCompressionFilter(implicit mat: Materializer): CompressionFilter = {
    new CompressionFilter(Vector("text/html", "application/json"), 1400)
  }

  @Provides
  def provideMetricsFilter(implicit mat: Materializer, ec: ExecutionContext): MetricsFilter = {
    new MetricsFilter()
  }

  @Provides
  def provideTraceFilter(implicit mat: Materializer, ec: ExecutionContext): TraceFilter = {
    new TraceFilter()
  }

  @Provides
  def provideMaintenanceFilter(implicit mat: Materializer, ec: ExecutionContext): MaintenanceModeFilter = {
    new MaintenanceModeFilter(Vector("/favicon.ico"))
  }

  @Provides
  @Singleton
  def provideEngineConnection(ws: WSClient, system: ActorSystem, environment: Environment)(implicit ec: ExecutionContext): WorkflowEngineConnection = {
    val storeCompanions = Map[ContainerType, JSONContainerCompanion](
      "Group" -> core.Group,
      "TransactionLog" -> core.TransactionLog,
      "LocalUser" -> core.LocalUser
    )

    environment.mode match {
      case Mode.Dev => if (!JSONConverter.isInitialized) JSONConverter.initialize(storeCompanions)
      case _ => JSONConverter.initialize(storeCompanions)
    }

    val engineConfig = StaticConfig.get.getConfig("engine")
    implicit val timeout = Timeout(engineConfig.getInt("requestTimeout").seconds)

    new WorkflowEngineConnection(
      system.actorOf(
        local.ServiceConnectionComponent.props(
          ws,
          StaticConfig.get.getConfig("security.authentication.services.LocalEngineExample-Users")
        )
      )
    )
  }

  @Provides
  @Singleton
  def provideDB(environment: Environment)(implicit system: ActorSystem, ec: ExecutionContext): DatabaseAbstractionLayer = {
    val storeCompanions = Map[ContainerType, JSONContainerCompanion](
      "LocalUser" -> core.LocalUser
    )

    environment.mode match {
      case Mode.Dev => if (!JSONConverter.isInitialized) JSONConverter.initialize(storeCompanions)
      case _ => JSONConverter.initialize(storeCompanions)
    }

    val storeConfig = StaticConfig.get.getConfig("database.redis")
    implicit val timeout = Timeout(StaticConfig.get.getInt("database.requestTimeout").seconds)

    val storeActor = system.actorOf(
      Redis.props(
        storeConfig.getString("hostname"),
        storeConfig.getInt("port"),
        storeConfig.getString("secret"),
        storeConfig.getInt("connectionTimeout"),
        storeCompanions,
        storeConfig.getInt("databaseID"),
        storeConfig.getInt("scanCount")
      )
    )

    new DatabaseAbstractionLayer(system.actorOf(Core.props(Map("LocalUser" -> Vector(storeActor)))))
  }

  @Provides
  @Singleton
  def provideComponentManager(engineConnection: WorkflowEngineConnection, db: DatabaseAbstractionLayer, system: ActorSystem)(implicit ec: ExecutionContext): ComponentManager = {
    val managerConfig = StaticConfig.get.getConfig("manager")
    implicit val timeout = Timeout(managerConfig.getInt("requestTimeout").seconds)

    new ComponentManager(
      system.actorOf(
        ComponentManagerActor.props(
          Map(
            "engine-connection" -> engineConnection.getRef,
            "db" -> db.getRef
          )
        )
      )
    )
  }

  @Provides
  @Singleton
  def provideLocalConsole(manager: ComponentManager, engineConnection: WorkflowEngineConnection)(implicit ec: ExecutionContext): Option[LocalConsole] = {
    Option(System.getProperty("c3eu.console")) match {
      case Some("enabled") =>
        val appVendor: String = core3_example_ui.BuildInfo.organization
        val appName: String = core3_example_ui.BuildInfo.name
        val appVersion: String = core3_example_ui.BuildInfo.version

        val managerConfig = StaticConfig.get.getConfig("manager")
        implicit val timeout = Timeout(managerConfig.getInt("requestTimeout").seconds)

        Some(
          LocalConsole(
            appVendor,
            appName,
            appVersion,
            manager.getRef,
            Seq(
              ComponentDescriptor("engine-connection", "Engine service connection", local.ServiceConnectionComponent),
              ComponentDescriptor("db", "Local user database", core3.database.dals.Core)
            )
          )
        )

      case _ => None
    }
  }

  @Provides
  @Singleton
  def provideWorkflows(): Seq[WorkflowBase] = {
    Seq(
      definitions.SystemCreateGroup,
      definitions.SystemCreateLocalUser,
      definitions.SystemDeleteGroup,
      definitions.SystemDeleteLocalUser,
      definitions.SystemQueryGroups,
      definitions.SystemQueryLocalUsers,
      definitions.SystemQueryTransactionLogs,
      definitions.SystemUpdateGroup,
      definitions.SystemUpdateLocalUserMetadata,
      definitions.SystemUpdateLocalUserPassword,
      definitions.SystemUpdateLocalUserPermissions
    )
  }
}
