module("utils", package.seeall)
_VERSION = "0.01"

function make_default_error_response()
    ngx.status = ngx.HTTP_INTERNAL_SERVER_ERROR
    return "InternalServerError"
end

default_timings_distribution = {
    {percentile=90, time=0.02},
    {percentile=99, time=0.05},
    {percentile=100, time=0.2}
}

default_errors_percent = 1

function slow_down_request(timings_distribution)
    current_timing = math.random(1,100);
    for i, timings in ipairs(timings_distribution) do
      if (current_timing <= timings.percentile) then
        passed_time = ngx.now() - ngx.req.start_time()
        if (passed_time < timings.time) then
            ngx.sleep(timings.time - passed_time)
        end
        break
      end
    end
end

-- make_ok_response - mandatory parameter, all others are optional
function respond(make_ok_response, make_err_response, timings_distribution, errors_percent)
    make_err_response = make_err_response or make_default_error_response
    timings_distribution = timings_distribution or default_timings_distribution
    errors_percent = errors_percent or default_errors_percent

    slow_down_request(timings_distribution)

    is_ok = math.random(1,100) > errors_percent;
    resp = ""
    if is_ok then
        resp = make_ok_response()
    else
        resp = make_err_response()
    end
    ngx.say(resp)
end
