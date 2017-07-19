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

import java.security.SecureRandom
import javax.inject.Inject

import containers.UserData
import core3.config.StaticConfig
import core3.database.{ObjectID, RevisionID, RevisionSequenceNumber}
import core3.database.containers.core
import core3.database.containers.core.LocalUser.UserType
import core3.database.dals.DatabaseAbstractionLayer
import core3.http.controllers.local.ClientController
import core3.http.requests.WorkflowEngineConnection
import core3.http.responses.GenericResult
import core3.workflows.{NoWorkflowParameters, WorkflowBase, WorkflowRequest}
import core3.workflows.definitions._
import play.api.Environment
import play.api.cache.SyncCacheApi
import play.api.data.Forms._
import play.api.data._
import play.api.data.format.Formatter
import play.api.libs.json.Json
import play.filters.csrf.CSRF

import scala.concurrent.{ExecutionContext, Future}

class Users @Inject()(engineConnection: WorkflowEngineConnection, cache: SyncCacheApi, db: DatabaseAbstractionLayer, workflows: Vector[WorkflowBase])
  (implicit ec: ExecutionContext, environment: Environment)
  extends ClientController(cache, StaticConfig.get.getConfig("security.authentication.clients.LocalUIExample"), db) {
  private val authConfig = StaticConfig.get.getConfig("security.authentication.clients.LocalUIExample")
  private val random = new SecureRandom()
  private val permissions = workflows.map(_.name) ++ Vector("c3eu:view", "c3eu:edit", "c3eu:delete", "exec:asUser", "exec:asClient")

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
            SystemQueryLocalUsers.name,
            NoWorkflowParameters().asJson
          )
        )
      } yield {
        if (result.wasSuccessful) {
          val users = result.data.map {
            output =>
              (output \ "users").as[Vector[core.LocalUser]]
          }.getOrElse(Seq.empty).sortWith(
            (a, b) =>
              a.created.isAfter(b.created)
          )

          val userData = UserData(user)

          Ok(views.html.users.page("Users", userData, users, permissions.sorted))
        } else {
          Ok(views.html.system.error("Users", result.message.getOrElse("No message provided"), Some(userData)))
        }
      }
    }
  )

  def create() = AuthorizedAction(
    "c3eu:edit",
    okHandler = { (request, user) => implicit val r = request
      Users.Forms.create.bindFromRequest.fold(
        form =>
          Future.successful(Ok(GenericResult(wasSuccessful = false, message = Some(s"Failed to validate input: [${form.errors}]")).asJson))
        ,
        params => {
          val (hashedPassword, salt) = core3.security.hashPassword(params.rawPassword, authConfig, random)
          val actualParams = SystemCreateLocalUser.SystemCreateLocalUserParameters(
            params.userID,
            hashedPassword,
            salt,
            params.permissions.toVector,
            params.userType,
            Json.obj(
              "first_name" -> params.firstName,
              "last_name" -> params.lastName
            )
          )

          for {
            result <- engineConnection.post(
              user,
              WorkflowRequest(
                SystemCreateLocalUser.name,
                actualParams.asJson,
                returnOutputData = true
              )
            )
          } yield {
            if (result.wasSuccessful) {
              val newUser = result.data match {
                case Some(data) => (data \ "add")(0).as[core.LocalUser]
                case None => throw new RuntimeException(s"Invalid response received from service.")
              }

              val userData = UserData(user)

              Ok(
                GenericResult(
                  wasSuccessful = true,
                  message = None,
                  data = Some(
                    Json.obj(
                      "html" -> views.html.users._user(newUser, userData, permissions, isNew = true).body
                    )
                  )
                ).asJson
              )
            } else {
              Ok(result.asJson)
            }
          }
        }
      )
    }
  )

  def delete() = AuthorizedAction(
    "c3eu:delete",
    okHandler = { (request, user) => implicit val r = request
      Users.Forms.delete.bindFromRequest.fold(
        form =>
          Future.successful(Ok(GenericResult(wasSuccessful = false, message = Some(s"Failed to validate input: [${form.errors}]")).asJson))
        ,
        params =>
          for {
            result <- engineConnection.post(user, WorkflowRequest(SystemDeleteLocalUser.name, params.asJson))
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

  def updatePassword() = AuthorizedAction(
    "c3eu:edit",
    okHandler = { (request, user) => implicit val r = request
      Users.Forms.updatePassword.bindFromRequest.fold(
        form =>
          Future.successful(Ok(GenericResult(wasSuccessful = false, message = Some(s"Failed to validate input: [${form.errors}]")).asJson))
        ,
        params => {
          val (hashedPassword, salt) = core3.security.hashPassword(params.rawPassword, authConfig, random)
          val actualParams = SystemUpdateLocalUserPassword.SystemUpdateLocalUserPasswordParameters(
            params.userUUID,
            params.revision,
            params.revisionNumber,
            hashedPassword,
            salt
          )

          for {

            result <- engineConnection.post(
              user,
              WorkflowRequest(
                SystemUpdateLocalUserPassword.name,
                actualParams.asJson,
                returnOutputData = true
              )
            )
          } yield {
            if (result.wasSuccessful) {
              val updatedUser = result.data match {
                case Some(data) => (data \ "update")(0).as[core.LocalUser]
                case None => throw new RuntimeException(s"Invalid response received from service.")
              }

              val userData = UserData(user)

              Ok(
                GenericResult(
                  wasSuccessful = true,
                  message = None,
                  data = Some(
                    Json.obj(
                      "html" -> views.html.users._user(updatedUser, userData, permissions, isNew = true).body
                    )
                  )
                ).asJson
              )
            } else {
              Ok(result.asJson)
            }
          }
        }
      )
    }
  )

  def updatePermissions() = AuthorizedAction(
    "c3eu:edit",
    okHandler = { (request, user) => implicit val r = request
      Users.Forms.updatePermissions.bindFromRequest.fold(
        form =>
          Future.successful(Ok(GenericResult(wasSuccessful = false, message = Some(s"Failed to validate input: [${form.errors}]")).asJson))
        ,
        params =>
          for {
            result <- engineConnection.post(
              user,
              WorkflowRequest(
                SystemUpdateLocalUserPermissions.name,
                params.asJson,
                returnOutputData = true
              )
            )
          } yield {
            if (result.wasSuccessful) {
              val updatedUser = result.data match {
                case Some(data) => (data \ "update")(0).as[core.LocalUser]
                case None => throw new RuntimeException(s"Invalid response received from service.")
              }

              val userData = UserData(user)

              Ok(
                GenericResult(
                  wasSuccessful = true,
                  message = None,
                  data = Some(
                    Json.obj(
                      "html" -> views.html.users._user(updatedUser, userData, permissions, isNew = true).body
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

  def updateMetadata() = AuthorizedAction(
    "c3eu:edit",
    okHandler = { (request, user) => implicit val r = request
      Users.Forms.updateMetadata.bindFromRequest.fold(
        form =>
          Future.successful(Ok(GenericResult(wasSuccessful = false, message = Some(s"Failed to validate input: [${form.errors}]")).asJson))
        ,
        params => {
          val actualParams = SystemUpdateLocalUserMetadata.SystemUpdateLocalUserMetadataParameters(
            params.userUUID,
            params.revision,
            params.revisionNumber,
            Json.obj(
              "first_name" -> params.firstName,
              "last_name" -> params.lastName
            )
          )

          for {
            result <- engineConnection.post(
              user,
              WorkflowRequest(
                SystemUpdateLocalUserMetadata.name,
                actualParams.asJson,
                returnOutputData = true
              )
            )
          } yield {
            if (result.wasSuccessful) {
              val updatedUser = result.data match {
                case Some(data) => (data \ "update")(0).as[core.LocalUser]
                case None => throw new RuntimeException(s"Invalid response received from service.")
              }

              val userData = UserData(user)

              Ok(
                GenericResult(
                  wasSuccessful = true,
                  message = None,
                  data = Some(
                    Json.obj(
                      "html" -> views.html.users._user(updatedUser, userData, permissions, isNew = true).body
                    )
                  )
                ).asJson
              )
            } else {
              Ok(result.asJson)
            }
          }
        }
      )
    }
  )
}

object Users {
  implicit def orgTypeFormat: Formatter[UserType] = new Formatter[UserType] {
    def bind(key: String, data: Map[String, String]) = data.get(key).map(UserType.fromString).toRight(Seq(FormError(key, "error.required", Nil)))

    override def unbind(key: String, value: UserType): Map[String, String] = Map(key -> value.toString)
  }

  case class CreateLocalUserIntermediateParameters(
    userID: String,
    rawPassword: String,
    permissions: Seq[String],
    userType: UserType,
    firstName: String,
    lastName: String
  )

  case class UpdateLocalUserPasswordIntermediateParameters(
    userUUID: ObjectID,
    revision: RevisionID,
    revisionNumber: RevisionSequenceNumber,
    rawPassword: String
  )

  case class UpdateLocalUserMetadataIntermediateParameters(
    userUUID: ObjectID,
    revision: RevisionID,
    revisionNumber: RevisionSequenceNumber,
    firstName: String,
    lastName: String
  )

  object Forms {
    val create = Form(
      mapping(
        "userID" -> nonEmptyText,
        "rawPassword" -> nonEmptyText,
        "permissions" -> seq(nonEmptyText),
        "userType" -> of[UserType],
        "firstName" -> nonEmptyText,
        "lastName" -> nonEmptyText
      )(CreateLocalUserIntermediateParameters.apply)(CreateLocalUserIntermediateParameters.unapply)
    )

    val delete = Form(
      mapping(
        "userUUID" -> uuid,
        "revision" -> uuid,
        "revisionNumber" -> number
      )(SystemDeleteLocalUser.SystemDeleteLocalUserParameters.apply)(SystemDeleteLocalUser.SystemDeleteLocalUserParameters.unapply)
    )

    val updatePassword = Form(
      mapping(
        "userUUID" -> uuid,
        "revision" -> uuid,
        "revisionNumber" -> number,
        "rawPassword" -> nonEmptyText
      )(UpdateLocalUserPasswordIntermediateParameters.apply)(UpdateLocalUserPasswordIntermediateParameters.unapply)
    )

    val updatePermissions = Form(
      mapping(
        "userUUID" -> uuid,
        "revision" -> uuid,
        "revisionNumber" -> number,
        "permissions" -> seq(nonEmptyText).transform[Vector[String]](s => s.toVector, v => v)
      )(SystemUpdateLocalUserPermissions.SystemUpdateLocalUserPermissionsParameters.apply)(SystemUpdateLocalUserPermissions.SystemUpdateLocalUserPermissionsParameters.unapply)
    )

    val updateMetadata = Form(
      mapping(
        "userUUID" -> uuid,
        "revision" -> uuid,
        "revisionNumber" -> number,
        "firstName" -> nonEmptyText,
        "lastName" -> nonEmptyText
      )(UpdateLocalUserMetadataIntermediateParameters.apply)(UpdateLocalUserMetadataIntermediateParameters.unapply)
    )
  }
}
