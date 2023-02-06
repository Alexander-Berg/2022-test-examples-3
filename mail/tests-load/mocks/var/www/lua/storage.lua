local utils = require "utils"
local messages = require "messages"

timings_distribution = {
    {percentile=50, time=0.02},
    {percentile=90, time=0.05},
    {percentile=95, time=0.07},
    {percentile=100, time=0.20}
}

function make_ok_response()
    return ngx.location.capture('/eml/' .. messages.select_message()).body
end

function make_error_response()
    ngx.status = ngx.HTTP_INTERNAL_SERVER_ERROR
    return 'no such file'
end

utils.respond(make_ok_response, make_error_response)
