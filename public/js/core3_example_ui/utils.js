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
define([],
    function() {
        function Utils() {
        }

        //Redirects the user to the login page, if their session has expired
        Utils.ajax = function (type, url, data, csrfToken) {
            return $.get("/system/status").then(
                function (result) {
                    var headers = {};
                    if(csrfToken) {
                        headers["Csrf-Token"] = csrfToken
                    }

                    return $.ajax({
                        type: type,
                        url: url,
                        headers: headers,
                        data: data,
                        dataType: "json"
                    });
                },
                function (xhr, status, error) {
                    window.location.href = "/system/login";
                    return {
                        wasSuccessful: false,
                        message: "Backend responded with failure."
                    };
                }
            );
        };

        Utils.prototype.get = function (url, data) {
            return Utils.ajax("GET", url, data);
        };

        Utils.prototype.post = function (url, data) {
            var csrfToken = $("#csrfToken").attr("data-token-value");
            return Utils.ajax("POST", url, data, csrfToken);
        };

        return new Utils();
    }
);