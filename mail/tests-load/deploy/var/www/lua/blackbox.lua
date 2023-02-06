local utils = require "utils"

function make_ok_response()
    return [[{
        "users":[{
            "uid": {
                "value": "100500"
            }
        }]
    }]]
end

function make_error_response()
    return [[{
        "error": "internal blackbox error"
    }]]
end

utils.respond(make_ok_response, make_error_response)
