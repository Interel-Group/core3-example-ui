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
    function () {
        function System() {}

        System.prototype.login = function () {
            var userControl = $("#c3eu-user-id");
            var passwordControl = $("#c3eu-user-password");
            var errorContainer = $("#c3eu-login-error-container");

            $("#c3eu-user-login-form").submit(function (e) {
                var errors = [];

                if (!userControl.val()) {
                    errors.push("User ID is required!");
                    userControl.addClass("uk-form-danger");
                } else {
                    userControl.removeClass("uk-form-danger");
                }

                if (!passwordControl.val()) {
                    errors.push("Password is required!");
                    passwordControl.addClass("uk-form-danger");
                } else {
                    passwordControl.removeClass("uk-form-danger");
                }

                if(errors.length > 0) {
                    errorContainer.html("");
                    errors.forEach(function (current) {
                        errorContainer.append($("<p></p>", {
                            text: current,
                            "class": "uk-alert-danger",
                            "uk-alert": ""
                        }));
                    });
                    errorContainer.removeClass("uk-hidden");
                } else {
                    errorContainer.addClass("uk-hidden");
                    errorContainer.html("");

                    var csrfToken = $("#csrfToken").attr("data-token-value");

                    $.ajax({
                        type: "POST",
                        url: "/system/login",
                        beforeSend: function (xhr) {
                            xhr.setRequestHeader ("Authorization", "Basic " + btoa(userControl.val() + ":" + passwordControl.val()));
                            xhr.setRequestHeader ("Csrf-Token", csrfToken);
                        }
                    }).done(function (result) {
                        if(result.wasSuccessful) {
                            window.location = "/";
                        } else {
                            errorContainer.removeClass("uk-hidden");
                            errorContainer.append($("<p></p>", {
                                text: "Authentication failure",
                                "class": "uk-alert-danger",
                                "uk-alert": ""
                            }));
                        }
                    }).fail(function (xhr, status, error) {
                        var message = xhr.responseJSON.message;
                        errorContainer.removeClass("uk-hidden");
                        errorContainer.append($("<p></p>", {
                            text: message || "Authentication failure",
                            "class": "uk-alert-danger",
                            "uk-alert": ""
                        }));
                    });
                }

                e.preventDefault();
            });
        };

        return new System();
    }
);