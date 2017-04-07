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

requirejs.config({
    baseUrl: '/public/js/core3_example_ui',
    paths: {}
});

requirejs(["system", "groups", "users", "utils"],
    function(system, groups, users, utils) {
        var path = window.location.pathname;
        switch (true) {
            case /\/system\/login/.test(path): system.login(); break;
            case /\/groups/.test(path): groups.init(); break;
            case /\/users/.test(path): users.init(); break;
            default: console.log("No handler found for path [" + path + "]."); break;
        }
    }
);