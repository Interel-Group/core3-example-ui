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
package containers

import core3.security.LocalAuthUserToken

/**
  * Basic user data container.
  *
  * @param name the name of the user
  * @param permissions the user's permissions
  */
case class UserData(
  name: String,
  permissions: Seq[String]
)

object UserData {
  def apply(token: LocalAuthUserToken): UserData = {
    val firstName = (token.profile \ "first_name").asOpt[String].getOrElse("-")
    val lastName = (token.profile \ "last_name").asOpt[String].getOrElse("-")

    new UserData(name = s"$firstName $lastName", token.permissions)
  }
}
