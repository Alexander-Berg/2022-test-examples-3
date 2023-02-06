local utils = require "utils"

function make_ok_response()
    return [[{
        "ok": true,
        "result": {
            "file_id": "id_123456",
            "file_unique_id": "unique_id_123456",
            "file_path": "123456/file/txt",
            "file_size": 0
        }
    }]]
end

function make_error_response()
    ngx.status = ngx.HTTP_INTERNAL_SERVER_ERROR
    return [[{
        "ok": false,
    }]]
end

utils.respond(make_ok_response, make_error_response)
