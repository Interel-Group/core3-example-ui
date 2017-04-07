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
        function Groups() {}

        Groups.clearForm = function (form) {
            form.get(0).reset();
        };

        Groups.createHandler = function (form, e) {
            var nameControl = $("#c3eu-group-name");
            var shortNameControl = $("#c3eu-group-short-name");
            var itemsTypeControl = $("#c3eu-group-items-type");

            var name = nameControl.val();
            var shortName = shortNameControl.val();
            var itemsType = itemsTypeControl.find("option:selected").val();

            var errors = false;
            if (!name) {
                errors = true;
                nameControl.addClass("uk-form-danger");
            } else {
                nameControl.removeClass("uk-form-danger");
            }

            if (!shortName) {
                errors = true;
                shortNameControl.addClass("uk-form-danger");
            } else {
                shortNameControl.removeClass("uk-form-danger");
            }

            if (!itemsType) {
                errors = true;
                itemsTypeControl.addClass("uk-form-danger");
            } else {
                itemsTypeControl.removeClass("uk-form-danger");
            }


            if (!errors) {
                UIkit.notification("<div uk-spinner></div> Updating...", {status: "primary", pos: "top-right"});

                utils.post(
                    "/groups/create",
                    {
                        "name": name,
                        "shortName": shortName,
                        "itemsType": itemsType,
                        "items": []
                    }
                ).done(function (createResult) {
                    if (createResult.wasSuccessful) {
                        $("#c3eu-group-grid").prepend(createResult.data.html);

                        UIkit.notification.closeAll();
                        UIkit.notification("<span uk-icon='icon: check'></span> Done.", {
                            status: "success",
                            pos: "top-right"
                        });
                    } else {
                        console.error("Operation failed: [" + createResult.message + "]");
                        UIkit.notification("<span uk-icon='icon: warning'></span> Failed to create group.", {
                            status: "danger",
                            pos: "top-right"
                        });
                    }
                }).fail(function (xhr, status, error) {
                    console.error("Operation failed with status [" + xhr.status + "] and message [" + error + "]");
                    UIkit.notification("<span uk-icon='icon: warning'></span> Failed to create group [" + xhr.status + "].", {
                        status: "danger",
                        pos: "top-right"
                    });
                });
            } else {
                e.stopPropagation();
                e.preventDefault();
            }
        };

        Groups.deleteHandler = function (e) {
            var groupContainer = $(e.currentTarget).closest(".c3eu-group-container");
            var objectData = groupContainer.find(".c3eu-group-object-data");
            var groupName = groupContainer.find(".c3eu-group-name").text();

            var id = objectData.attr("data-id");
            var revision = objectData.attr("data-revision");
            var revisionNumber = objectData.attr("data-revisionNumber");

            UIkit.modal.confirm("Remove group [" + groupName + "]?").then(
                function () {
                    UIkit.notification("<div uk-spinner></div> Updating...", {status: "primary", pos: "top-right"});

                    utils.post(
                        "/groups/delete",
                        {
                            "groupID": id,
                            "revision": revision,
                            "revisionNumber": revisionNumber
                        }
                    ).done(function (result) {
                        if (result.wasSuccessful) {
                            groupContainer.parent().remove();

                            UIkit.notification.closeAll();
                            UIkit.notification("<span uk-icon='icon: check'></span> Done.", {status: "success", pos: "top-right"});
                        } else {
                            console.error("Operation failed: [" + result.message + "]");
                            UIkit.notification("<span uk-icon='icon: warning'></span> Failed to delete group.", {status: "danger", pos: "top-right"});
                        }
                    }).fail(function (xhr, status, error) {
                        console.error("Operation failed with status [" + xhr.status + "] and message [" + error + "]");
                        UIkit.notification("<span uk-icon='icon: warning'></span> Failed to delete group [" + xhr.status + "].", {
                            status: "danger",
                            pos: "top-right"
                        });
                    });
                },
                function () {
                });
        };

        Groups.editHandler = function (e) {
            var groupContainer = $(e.currentTarget).closest(".c3eu-group-container");

            var name = groupContainer.find(".c3eu-group-name");

            var originalName = name.text();

            var groupButtonContainer = groupContainer.find(".c3eu-group-button-container");
            var groupEditButton = groupButtonContainer.find(".c3eu-group-edit");
            var groupDeleteButton = groupButtonContainer.find(".c3eu-group-delete");

            var groupSaveButton = $("<a></a>", {
                title: "Save",
                "class": "uk-icon-button uk-text-success c3eu-group-save",
                "uk-icon": "icon: check",
                "uk-tooltip": "",
                click: function () {
                    groupButtonContainer.find(".c3eu-group-save").addClass("uk-disabled");

                    var objectData = groupContainer.find(".c3eu-group-object-data");

                    var id = objectData.attr("data-id");
                    var revision = objectData.attr("data-revision");
                    var revisionNumber = objectData.attr("data-revisionNumber");

                    UIkit.notification("<div uk-spinner></div> Updating...", {status: "primary", pos: "top-right"});

                    utils.post(
                        "/groups/update",
                        {
                            "groupID": id,
                            "revision": revision,
                            "revisionNumber": revisionNumber,
                            "name": name.text()
                        }
                    ).done(function (updateResult) {
                        if (updateResult.wasSuccessful) {
                            groupContainer.parent().replaceWith(updateResult.data.html);

                            UIkit.notification.closeAll();
                            UIkit.notification("<span uk-icon='icon: check'></span> Done.", {
                                status: "success",
                                pos: "top-right"
                            });
                        } else {
                            console.error("Operation failed: [" + updateResult.message + "]");
                            UIkit.notification("<span uk-icon='icon: warning'></span> Failed to update group.", {
                                status: "danger",
                                pos: "top-right"
                            });
                        }
                    }).fail(function (xhr, status, error) {
                        console.error("Operation failed with status [" + xhr.status + "] and message [" + error + "]");
                        UIkit.notification("<span uk-icon='icon: warning'></span> Failed to update group [" + xhr.status + "].", {
                            status: "danger",
                            pos: "top-right"
                        });
                    });
                }
            });

            var groupCancelButton = $("<a></a>", {
                title: "Cancel",
                "class": "uk-icon-button uk-text-danger c3eu-group-cancel",
                "uk-icon": "icon: close",
                "uk-tooltip": "",
                click: function () {
                    name.text(originalName);

                    name.removeClass("c3eu-editable");
                    name.attr("contentEditable", false);
                    groupEditButton.removeClass("uk-hidden");
                    groupDeleteButton.removeClass("uk-hidden");
                    groupContainer.find(".c3eu-group-save").remove();
                    groupContainer.find(".c3eu-group-cancel").remove();
                }
            });

            name.addClass("c3eu-editable");
            name.attr("contentEditable", true);
            groupEditButton.addClass("uk-hidden");
            groupDeleteButton.addClass("uk-hidden");
            groupButtonContainer.append(groupSaveButton);
            groupButtonContainer.append(groupCancelButton);
        };

        Groups.prototype.init = function () {
            var grid = $("#c3eu-group-grid");

            $("body").on("hide", "[uk-modal]", function (e) {
                Groups.clearForm($(e.currentTarget).find("form"))
            });

            $(".c3eu-group-add-create").click(function (e) {
                Groups.createHandler($("#c3eu-group-add-form"), e);
            });

            grid.on("click", ".c3eu-group-delete", Groups.deleteHandler);
            grid.on("click", ".c3eu-group-edit", Groups.editHandler);
        };

        return new Groups();
    }
);