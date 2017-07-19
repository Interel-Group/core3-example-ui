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
package controllers

import javax.inject.Inject

import containers.UserData
import core3.config.StaticConfig
import core3.database.ObjectID
import core3.database.containers.core
import core3.database.dals.DatabaseAbstractionLayer
import core3.http.controllers.local.ClientController
import core3.http.requests.WorkflowEngineConnection
import core3.http.responses.GenericResult
import core3.workflows.{NoWorkflowParameters, WorkflowRequest}
import core3.workflows.definitions._
import play.api.Environment
import play.api.cache.SyncCacheApi
import play.api.data.Forms._
import play.api.data._
import play.api.libs.json.Json
import play.filters.csrf.CSRF

import scala.concurrent.{ExecutionContext, Future}

class Groups @Inject()(engineConnection: WorkflowEngineConnection, cache: SyncCacheApi, db: DatabaseAbstractionLayer)
  (implicit ec: ExecutionContext, environment: Environment)
  extends ClientController(cache, StaticConfig.get.getConfig("security.authentication.clients.LocalUIExample"), db) {

  //Groups page handler - retrieves all groups, sorts them by creation date and renders them
  def page() = AuthorizedAction(
    "c3eu:view",
    okHandler = { (request, user) =>
      implicit val r = request
      implicit val token = CSRF.getToken

      val userData = UserData(user)
      for {
        result <- engineConnection.post(
          user,
          WorkflowRequest(
            SystemQueryGroups.name,
            NoWorkflowParameters().asJson
          )
        )
      } yield {
        if (result.wasSuccessful) {
          val groups = result.data.map {
            output =>
              (output \ "groups").as[Vector[core.Group]]
          }.getOrElse(Seq.empty).sortWith(
            (a, b) =>
              a.created.isAfter(b.created)
          )

          val userData = UserData(user)

          Ok(views.html.groups.page("Groups", userData, groups))
        } else {
          Ok(views.html.system.error("Groups", result.message.getOrElse("No message provided"), Some(userData)))
        }
      }
    }
  )

  //Groups creation handler - processes the supplied data, sends a request to the workflow engine and renders the new object, ready to be inserted into the DOM
  def create() = AuthorizedAction(
    "c3eu:edit",
    okHandler = { (request, user) => implicit val r = request
      Groups.Forms.create.bindFromRequest.fold(
        form =>
          Future.successful(Ok(GenericResult(wasSuccessful = false, message = Some(s"Failed to validate input: [${form.errors}]")).asJson))
        ,
        params =>
          for {
            result <- engineConnection.post(
              user,
              WorkflowRequest(
                SystemCreateGroup.name,
                params.asJson,
                returnOutputData = true
              )
            )
          } yield {
            if (result.wasSuccessful) {
              val group = result.data match {
                case Some(data) => (data \ "add")(0).as[core.Group]
                case None => throw new RuntimeException(s"Invalid response received from service.")
              }

              val userData = UserData(user)

              Ok(
                GenericResult(
                  wasSuccessful = true,
                  message = None,
                  data = Some(
                    Json.obj(
                      "html" -> views.html.groups._group(group, userData, isNew = true).body
                    )
                  )
                ).asJson
              )
            } else {
              Ok(result.asJson)
            }
          }
      )
    }
  )

  //Groups deletion handler - processes the supplied data, sends a request to the workflow engine and responds with a generic result
  def delete() = AuthorizedAction(
    "c3eu:delete",
    okHandler = { (request, user) => implicit val r = request
      Groups.Forms.delete.bindFromRequest.fold(
        form =>
          Future.successful(Ok(GenericResult(wasSuccessful = false, message = Some(s"Failed to validate input: [${form.errors}]")).asJson))
        ,
        params =>
          for {
            result <- engineConnection.post(user, WorkflowRequest(SystemDeleteGroup.name, params.asJson))
          } yield {
            if (result.wasSuccessful) {
              Ok(GenericResult(wasSuccessful = true).asJson)
            } else {
              Ok(result.asJson)
            }
          }
      )
    }
  )

  //Groups update handler - processes the supplied data, sends a request to the workflow engine and renders the updated object, ready to be re-inserted into the DOM
  def update() = AuthorizedAction(
    "c3eu:edit",
    okHandler = { (request, user) => implicit val r = request
      Groups.Forms.update.bindFromRequest.fold(
        form =>
          Future.successful(Ok(GenericResult(wasSuccessful = false, message = Some(s"Failed to validate input: [${form.errors}]")).asJson))
        ,
        params =>
          for {
            result <- engineConnection.post(
              user,
              WorkflowRequest(
                SystemUpdateGroup.name,
                params.asJson,
                returnOutputData = true
              )
            )
          } yield {
            if (result.wasSuccessful) {
              val group = result.data match {
                case Some(data) => (data \ "update")(0).as[core.Group]
                case None => throw new RuntimeException(s"Invalid response received from service.")
              }

              val userData = UserData(user)

              Ok(
                GenericResult(
                  wasSuccessful = true,
                  message = None,
                  data = Some(
                    Json.obj(
                      "html" -> views.html.groups._group(group, userData, isNew = true).body
                    )
                  )
                ).asJson
              )
            } else {
              Ok(result.asJson)
            }
          }
      )
    }
  )
}

object Groups {
  object Forms {
    val create = Form(
      mapping(
        "shortName" -> nonEmptyText,
        "name" -> nonEmptyText,
        "items" -> list(uuid).transform[Vector[ObjectID]](_.toVector, _.toList),
        "itemsType" -> nonEmptyText
      )(SystemCreateGroup.SystemAddGroupParameters.apply)(SystemCreateGroup.SystemAddGroupParameters.unapply)
    )

    val delete = Form(
      mapping(
        "groupID" -> uuid,
        "revision" -> uuid,
        "revisionNumber" -> number
      )(SystemDeleteGroup.SystemDeleteGroupParameters.apply)(SystemDeleteGroup.SystemDeleteGroupParameters.unapply)
    )

    val update = Form(
      mapping(
        "groupID" -> uuid,
        "revision" -> uuid,
        "revisionNumber" -> number,
        "name" -> optional(nonEmptyText),
        "items" -> optional(list(uuid).transform[Vector[ObjectID]](_.toVector, _.toList))
      )(SystemUpdateGroup.SystemUpdateGroupParameters.apply)(SystemUpdateGroup.SystemUpdateGroupParameters.unapply)
    )
  }
}
