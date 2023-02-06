local luajava = require 'luajava';
local LuaClient = luajava.bindClass("ru.yandex.antifraud.artefacts.LuaClient");
local YandexHeaders = luajava.bindClass("ru.yandex.http.util.YandexHeaders");

local module = {};

local CLIENT_NAME = "TrustClient";

function module.wallet_balance(context, uid, callback)
    local ok, error = pcall(function()
        assert(uid);
        local request = LuaClient:query(
            "/legacy/wallet-balance?",
            {
                uid = uid
            }
        );
        local headers = {
            [YandexHeaders.X_REQUEST_ID] = context.nsrc.txn_extid
        }

        context.client:get(CLIENT_NAME,
            request,
            headers,
            callback);
    end)
    if not ok then
        context.comment(error);
    end
end

return module;
