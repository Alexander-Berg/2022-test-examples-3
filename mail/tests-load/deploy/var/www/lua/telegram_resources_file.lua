local utils = require "utils"

function make_ok_response()
    return ngx.location.capture('/static/telegram_stub_file.txt').body
end

utils.respond(make_ok_response)
