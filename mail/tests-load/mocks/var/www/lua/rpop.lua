local utils = require "utils"

function make_ok_response()
    return [[
{
"request":"list",
"host":"rpop1j.mail.yandex.net",
"id":"cF0j1J1MnmI1",
"rpops":[
    {
    "popid":"24002358302",
    "server":"imap.yandex.ru",
    "port":"993",
    "login":"contact@irinaverdier.com",
    "use_ssl":true,
    "email":"contact@irinaverdier.com",
    "is_on":"1",
    "last_connect":"1554936907",
    "session_duration":"1",
    "bad_retries":0,
    "error_status":"ok",
    "last_msg_count":0,
    "leave_msgs":true,
    "abook_sync_state":"0",
    "imap":true,"root_folder":"",
    "label_id":"",
    "is_oauth":false,
    "actions":["0"]
    }
    ]
}
]]
end

function make_error_response()
    return '{"error":{"method":"list","reason":"syntax error","description":"can`t parse request","host":"rpop1j.mail.yandex.net","request_id":"0I0SnL1MnOs1"}}'
end

utils.respond(make_ok_response, make_error_response)
