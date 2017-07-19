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
import core3.database.dals.DatabaseAbstractionLayer
import core3.http.controllers.local.ClientController
import core3.http.requests.WorkflowEngineConnection
import core3.http.responses.GenericResult
import play.api.Environment
import play.api.cache.SyncCacheApi
import play.api.libs.json.Json
import play.filters.csrf.CSRF

import scala.concurrent.{ExecutionContext, Future}

class System @Inject()(engineConnection: WorkflowEngineConnection, cache: SyncCacheApi, db: DatabaseAbstractionLayer)
  (implicit ec: ExecutionContext, environment: Environment)
  extends ClientController(cache, StaticConfig.get.getConfig("security.authentication.clients.LocalUIExample"), db) {

  //Example page showcasing redirecting based on whether the user is authenticated or not
  def root() = PublicAction(
    { (request, user) => implicit val r = request
      user match {
        case Some(_) => Future.successful(Redirect("/internal"))
        case None => Future.successful(Redirect("/public"))
      }
    }
  )

  //Example page showcasing different rendering based on whether the user is authenticated or not
  def public() = PublicAction(
    { (request, user) => implicit val r = request
      val userData = user.map(UserData.apply)
      Future.successful(Ok(views.html.system.public("Example - Public", userData)))
    }
  )

  //Example page available to authenticated users only
  def internal() = AuthorizedAction(
    "c3eu:view",
    okHandler = { (request, user) => implicit val r = request
      val userData = UserData(user)
      Future.successful(Ok(views.html.system.internal("Example - Internal", userData)))
    }
  )

  /**
    * Handler used for determining whether the user is authenticated
    *
    * Used by JS to redirect the user to the login page, if their session has expired, rather than getting a 401.
    */
  def status() = AuthorizedAction(
    "c3eu:view",
    okHandler = { (request, _) => implicit val r = request
      Future.successful(Ok(Json.obj("auth" -> "ok")))
    }
  )

  //Login page handler that redirects to "/", if the user has already logged in.
  def loginPage = PublicAction(
    { (request, user) =>
      implicit val r = request
      implicit val token = CSRF.getToken

      user match {
        case Some(_) => Future.successful(Redirect("/"))
        case None => Future.successful(Ok(views.html.system.login("Login")))
      }
    }
  )

  //Login action handler
  def login() = LoginAction(
    { implicit request => //success
      Future.successful(Ok(GenericResult(wasSuccessful = true).asJson))
    }, { implicit request => //not allowed
      Future.successful(Unauthorized(GenericResult(wasSuccessful = false, message = Some(s"Invalid user and/or password")).asJson))
    }, { implicit request => //should log in
      Future.successful(Unauthorized(GenericResult(wasSuccessful = false, message = Some(s"Login required")).asJson))
    }
  )

  //Logout action handler
  def logout() = LogoutAction()
}
