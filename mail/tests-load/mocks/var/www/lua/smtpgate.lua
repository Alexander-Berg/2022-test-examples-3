local utils = require "utils"

timings_distribution = {
    {percentile=50, time=0.774},
    {percentile=90, time=1.10},
    {percentile=95, time=1.30},
    {percentile=100, time=2.20}
}

function make_ok_response()
    return '{ "error": "Success", "mid": "123456789", "explanation": "" }'
end

function make_error_response()
    ngx.status = ngx.HTTP_INTERNAL_SERVER_ERROR
    return '{ "error": "SendMessageFailed", "mid": "", "explanation": "error description" }'
end

utils.respond(make_ok_response, make_error_response, timings_distribution)
