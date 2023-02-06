local luajava = require 'luajava';
local LuaClient = luajava.bindClass("ru.yandex.antifraud.artefacts.LuaClient");

local module = {};

local CLIENT_NAME = "PharmaClient";

function module.phone_number_factors(context, phone_number, callback)
    local ok, error = pcall(function()
        assert(phone_number);
        local request = LuaClient:query("/v1/factors_by_number", {});
        local data = LuaClient:url_encoded({
            phone_number = phone_number
        });

        context.client:post(CLIENT_NAME, request, data, callback);
    end)
    if not ok then
        context.comment(error);
    end
end

return module;
