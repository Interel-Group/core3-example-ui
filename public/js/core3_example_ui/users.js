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
define(["utils"],
    function (utils) {
        function Users() {}

        Users.clearForm = function (form) {
            form.get(0).reset();
        };

        Users.createHandler = function (form, e) {
            var idControl = $("#c3eu-user-id");
            var passwordControl = $("#c3eu-user-password");
            var passwordConfirmControl = $("#c3eu-user-password-confirm");
            var permissionsControl = $("#c3eu-user-permissions");
            var userTypeControl = $("#c3eu-user-type");
            var firstNameControl = $("#c3eu-user-first-name");
            var lastNameControl = $("#c3eu-user-last-name");

            var userID = idControl.val();
            var password = passwordControl.val();
            var passwordConfirm = passwordConfirmControl.val();
            var permissions = permissionsControl.find("option:selected").val();
            var userType = userTypeControl.find("option:selected").val();
            var firstName = firstNameControl.val();
            var lastName = lastNameControl.val();

            var errors = false;
            if (!userID) {
                errors = true;
                idControl.addClass("uk-form-danger");
            } else {
                idControl.removeClass("uk-form-danger");
            }

            if (!passwordControl || !passwordConfirm || password !== passwordConfirm) {
                errors = true;
                passwordControl.addClass("uk-form-danger");
                passwordConfirmControl.addClass("uk-form-danger");
            } else {
                passwordControl.removeClass("uk-form-danger");
                passwordConfirmControl.removeClass("uk-form-danger");
            }

            if (!permissions) {
                errors = true;
                permissionsControl.addClass("uk-form-danger");
            } else {
                permissionsControl.removeClass("uk-form-danger");
            }

            if (!userType) {
                errors = true;
                userTypeControl.addClass("uk-form-danger");
            } else {
                userTypeControl.removeClass("uk-form-danger");
            }

            if (!firstName) {
                errors = true;
                firstNameControl.addClass("uk-form-danger");
            } else {
                firstNameControl.removeClass("uk-form-danger");
            }

            if (!lastName) {
                errors = true;
                lastNameControl.addClass("uk-form-danger");
            } else {
                lastNameControl.removeClass("uk-form-danger");
            }


            if (!errors) {
                UIkit.notification("<div uk-spinner></div> Updating...", {status: "primary", pos: "top-right"});

                utils.post(
                    "/users/create",
                    {
                        "userID": userID,
                        "rawPassword": password,
                        "permissions": permissions,
                        "userType": userType,
                        "firstName": firstName,
                        "lastName": lastName
                    }
                ).done(function (createResult) {
                    if (createResult.wasSuccessful) {
                        $("#c3eu-user-grid").prepend(createResult.data.html);

                        UIkit.notification.closeAll();
                        UIkit.notification("<span uk-icon='icon: check'></span> Done.", {
                            status: "success",
                            pos: "top-right"
                        });
                    } else {
                        console.error("Operation failed: [" + createResult.message + "]");
                        UIkit.notification("<span uk-icon='icon: warning'></span> Failed to create user.", {
                            status: "danger",
                            pos: "top-right"
                        });
                    }
                }).fail(function (xhr, status, error) {
                    console.error("Operation failed with status [" + xhr.status + "] and message [" + error + "]");
                    UIkit.notification("<span uk-icon='icon: warning'></span> Failed to create user [" + xhr.status + "].", {
                        status: "danger",
                        pos: "top-right"
                    });
                });
            } else {
                e.stopPropagation();
                e.preventDefault();
            }
        };

        Users.deleteHandler = function (e) {
            var userContainer = $(e.currentTarget).closest(".c3eu-user-container");
            var objectData = userContainer.find(".c3eu-user-object-data");
            var userID = userContainer.find(".c3eu-user-id").text();

            var id = objectData.attr("data-id");
            var revision = objectData.attr("data-revision");
            var revisionNumber = objectData.attr("data-revisionNumber");

            UIkit.modal.confirm("Remove user [" + userID + "]?").then(
                function () {
                    UIkit.notification("<div uk-spinner></div> Updating...", {status: "primary", pos: "top-right"});

                    utils.post(
                        "/users/delete",
                        {
                            "userUUID": id,
                            "revision": revision,
                            "revisionNumber": revisionNumber
                        }
                    ).done(function (result) {
                        if (result.wasSuccessful) {
                            userContainer.parent().remove();

                            UIkit.notification.closeAll();
                            UIkit.notification("<span uk-icon='icon: check'></span> Done.", {status: "success", pos: "top-right"});
                        } else {
                            console.error("Operation failed: [" + result.message + "]");
                            UIkit.notification("<span uk-icon='icon: warning'></span> Failed to delete user.", {status: "danger", pos: "top-right"});
                        }
                    }).fail(function (xhr, status, error) {
                        console.error("Operation failed with status [" + xhr.status + "] and message [" + error + "]");
                        UIkit.notification("<span uk-icon='icon: warning'></span> Failed to delete user [" + xhr.status + "].", {
                            status: "danger",
                            pos: "top-right"
                        });
                    });
                },
                function () {
                });
        };

        Users.editHandler = function (e) {
            var userContainer = $(e.currentTarget).closest(".c3eu-user-container");

            var firstName = userContainer.find(".c3eu-user-first-name");
            var lastName = userContainer.find(".c3eu-user-last-name");

            var originalFirstName = firstName.text();
            var originalLastName = lastName.text();

            var userButtonContainer = userContainer.find(".c3eu-user-button-container");
            var userEditButton = userButtonContainer.find(".c3eu-user-edit");
            var userPasswordResetButton = userButtonContainer.find(".c3eu-user-reset-password");
            var userDeleteButton = userButtonContainer.find(".c3eu-user-delete");

            var userSaveButton = $("<a></a>", {
                title: "Save",
                "class": "uk-icon-button uk-text-success c3eu-user-save",
                "uk-icon": "icon: check",
                "uk-tooltip": "",
                click: function () {
                    userButtonContainer.find(".c3eu-user-save").addClass("uk-disabled");

                    var objectData = userContainer.find(".c3eu-user-object-data");

                    var id = objectData.attr("data-id");
                    var revision = objectData.attr("data-revision");
                    var revisionNumber = objectData.attr("data-revisionNumber");

                    UIkit.notification("<div uk-spinner></div> Updating...", {status: "primary", pos: "top-right"});

                    utils.post(
                        "/users/update-metadata",
                        {
                            "userUUID": id,
                            "revision": revision,
                            "revisionNumber": revisionNumber,
                            "firstName": firstName.text(),
                            "lastName": lastName.text()
                        }
                    ).done(function (updateResult) {
                        if (updateResult.wasSuccessful) {
                            userContainer.parent().replaceWith(updateResult.data.html);

                            UIkit.notification.closeAll();
                            UIkit.notification("<span uk-icon='icon: check'></span> Done.", {
                                status: "success",
                                pos: "top-right"
                            });
                        } else {
                            console.error("Operation failed: [" + updateResult.message + "]");
                            UIkit.notification("<span uk-icon='icon: warning'></span> Failed to update user.", {
                                status: "danger",
                                pos: "top-right"
                            });
                        }
                    }).fail(function (xhr, status, error) {
                        console.error("Operation failed with status [" + xhr.status + "] and message [" + error + "]");
                        UIkit.notification("<span uk-icon='icon: warning'></span> Failed to update user [" + xhr.status + "].", {
                            status: "danger",
                            pos: "top-right"
                        });
                    });
                }
            });

            var userCancelButton = $("<a></a>", {
                title: "Cancel",
                "class": "uk-icon-button uk-text-danger c3eu-user-cancel",
                "uk-icon": "icon: close",
                "uk-tooltip": "",
                click: function () {
                    firstName.text(originalFirstName);
                    lastName.text(originalLastName);

                    firstName.removeClass("c3eu-editable");
                    firstName.attr("contentEditable", false);
                    lastName.removeClass("c3eu-editable");
                    lastName.attr("contentEditable", false);
                    userEditButton.removeClass("uk-hidden");
                    userPasswordResetButton.removeClass("uk-hidden");
                    userDeleteButton.removeClass("uk-hidden");
                    userContainer.find(".c3eu-user-save").remove();
                    userContainer.find(".c3eu-user-cancel").remove();
                }
            });

            firstName.addClass("c3eu-editable");
            firstName.attr("contentEditable", true);
            lastName.addClass("c3eu-editable");
            lastName.attr("contentEditable", true);
            userEditButton.addClass("uk-hidden");
            userPasswordResetButton.addClass("uk-hidden");
            userDeleteButton.addClass("uk-hidden");
            userButtonContainer.append(userSaveButton);
            userButtonContainer.append(userCancelButton);
        };

        Users.prototype.init = function () {
            var grid = $("#c3eu-user-grid");

            $("body").on("hide", "[uk-modal]", function (e) {
                Users.clearForm($(e.currentTarget).find("form"))
            });

            $(".c3eu-user-add-create").click(function (e) {
                Users.createHandler($("#c3eu-user-add-form"), e);
            });

            grid.on("click", ".c3eu-user-delete", Users.deleteHandler);
            grid.on("click", ".c3eu-user-edit", Users.editHandler);

            //TODO - handle password reset
            //TODO - handle permissions update
        };

        return new Users();
    }
);