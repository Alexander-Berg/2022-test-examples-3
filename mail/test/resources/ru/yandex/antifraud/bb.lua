local luajava = require 'luajava';
local LuaClient = luajava.bindClass("ru.yandex.antifraud.artefacts.LuaClient");

local module = {};

local CLIENT_NAME = "BlackboxClient";

function module.userinfo(context, uid, callback)
    local ok, error = pcall(function()
        assert(uid);
        local request = LuaClient:query(
                "/blackbox/?",
                {
                    format = "json",
                    method = "userinfo",
                    userip = "127.0.0.1",
                    aliases = "1,2,3,5,6,7,8,9,10,11,12,13,15,16,17,18,19,20,21,22",
                    attributes = "1,31,107,110,132,200,1003,1015",
                    getphones = "bound",
                    phone_attributes = "102,104,106,108",
                    sid = 2,
                    uid = uid
                }
        );

        context.client:get(CLIENT_NAME, request, function(err, response)
            if err then
                return callback(err);
            else
                return callback(nil, response.users[1]);
            end
        end);
    end)
    if not ok then
        context.comment(error);
    end
end

return module;
