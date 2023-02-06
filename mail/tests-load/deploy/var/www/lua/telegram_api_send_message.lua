local utils = require "utils"

function make_ok_response()
    return [[{
        "ok": true
    }]]
end

function make_error_response()
    ngx.status = ngx.HTTP_INTERNAL_SERVER_ERROR
    return [[{
        "ok": false,
    }]]
end

utils.respond(make_ok_response, make_error_response)
