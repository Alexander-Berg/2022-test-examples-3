local luajava = require 'luajava';
local lua_defaults = require 'lua-defaults';

local URLEncoder = luajava.bindClass("java.net.URLEncoder");

local module = {};

local CLIENT_NAME = "PassportClient";
local DEDUPLICATED_CLIENT_NAME = "DeduplicatedPassportClient";

local RETPATH_BY_SERVICE = {
    ["PAY"] = "https://pay.yandex.ru"
}

--https://wiki.yandex-team.ru/passport/api/bundle/auth/challenge/#createtrack
function module.create_track(context, callback)
    local ok, error = pcall(function()
        local data = {
            uid = context.nsrc.uid,
            retpath = context.src.retpath or RETPATH_BY_SERVICE[context.src.service] or "https://yandex.ru",
            card_id_for_3ds = context.nsrc.card_id
        };

        context.client:post(CLIENT_NAME,
            context.client:query("/1/bundle/challenge/standalone/create_track/?", {
                consumer = lua_defaults.passport.consumer,
            }),
            context.client:url_encoded(data),
            callback);
    end)
    if not ok then
        context.comment(error);
    end
end

function module.change_password(context,
                                comment,
                                author,
                                max_change_frequency_in_days,
                                notify_by_sms,
                                show_2fa_promo
)
    local ok, error = pcall(function()
        local data = {
            admin_name = author,
            comment = comment,
            is_changing_required = 1,
            notify_by_sms = notify_by_sms,
            show_2fa_promo = show_2fa_promo,
            antifraud_external_id = context.nsrc.txn_extid
        };

        if max_change_frequency_in_days and max_change_frequency_in_days > 0 then
            data.max_change_frequency_in_days = max_change_frequency_in_days;
        end

        context.client:post(CLIENT_NAME,
            context.client:query("/2/account/"
                .. URLEncoder:encode(context.nsrc.uid)
                .. "/password_options/?", {
                consumer = lua_defaults.passport.consumer,
                action = "change_password"
            }),
            context.client:url_encoded(data));
    end)
    if not ok then
        context.comment(error);
    end
end

function module.send_custom_push(args)
    local ok, error = pcall(function()
        local data = {
            uid = args.uid,
            push_service = args.push_service,
            event_name = args.event_name,
            title = args.title,
        };

        if args.require_trusted_device then
            data.require_trusted_device = args.require_trusted_device;
        end

        if args.check_subscriptions then
            data.check_subscriptions = args.check_subscriptions;
        end

        if args.body then
            data.body = args.body;
        end

        if args.subtitle then
            data.subtitle = args.subtitle;
        end

        if args.webview_url then
            data.webview_url = args.webview_url;
        end

        if args.require_web_auth then
            data.require_web_auth = args.require_web_auth;
        end

        local headers = {
            ZooHash = args.zoo_hash,
            ZooShardId = "0",
            service = "so_fraud_pushes",
            ["Check-Duplicate"] = "1",
        };

        args.context.log('push_data', data);

        args.context.client:post(DEDUPLICATED_CLIENT_NAME,
            args.context.client:query("/1/bundle/push/send/am/?", {
                consumer = args.context.settings.passport_consumer or lua_defaults.passport.consumer,
                antifraud_external_id = args.context.nsrc.txn_extid,
                uid = args.context.uid,
            }),
            args.context.client:url_encoded(data),
            headers);
    end)
    if not ok then
        args.context.comment(error);
    end
end

function module.logout(context,
                       comment,
                       author
)
    local ok, error = pcall(function()
        local data = {
            admin_name = author,
            comment = comment,
            global_logout = 1,
        };

        context.client:post(CLIENT_NAME,
            context.client:query("/2/account/" ..
                URLEncoder:encode(context.nsrc.uid) ..
                "/password_options/?", {
                consumer = lua_defaults.passport.consumer,
                antifraud_external_id = context.nsrc.txn_extid,
                action = "logout"
            }),
            context.client:url_encoded(data));
    end)
    if not ok then
        context.comment(error);
    end
end

function module.send_mail(context, is_challenged)
    local ok, error = pcall(function()
        local data = {
            uid = context.nsrc.uid,
            device_id = context.nsrc.device_id,
            is_challenged = is_challenged
        };

        context.client:post(CLIENT_NAME,
            context.client:query("/1/bundle/auth/password/challenge/send_email/?", {
                consumer = lua_defaults.passport.consumer,
                antifraud_external_id = context.nsrc.txn_extid,
            }),
            context.client:url_encoded(data),
            {
                ["Ya-Client-User-Agent"] = context.nsrc.user_agent,
                ["Ya-Consumer-Client-Ip"] = context.nsrc.ip,
            });
    end)
    if not ok then
        context.comment(error);
    end
end

function module.send_push(context)
    local ok, error = pcall(function()
        local data = {
            uid = context.nsrc.uid,
            device_id = context.nsrc.device_id
        };

        context.client:post(CLIENT_NAME,
            context.client:query("/1/bundle/auth/password/challenge/send_push/?", {
                consumer = lua_defaults.passport.consumer,
                antifraud_external_id = context.nsrc.txn_extid,
            }),
            context.client:url_encoded(data),
            {
                ["Ya-Client-User-Agent"] = context.nsrc.user_agent,
                ["Ya-Consumer-Client-Ip"] = context.nsrc.ip,
            });
    end)
    if not ok then
        context.comment(error);
    end
end

function module.setup_prod(app)
    if not app.settings then
        app.settings = {};
    end
    app.settings.passport_consumer = "SoFraud";
    app.settings.passport_host = "https://passport.yandex.ru";
end

function module.setup_testing(app)
    if not app.settings then
        app.settings = {};
    end
    app.settings.passport_consumer = "SoFraudTesting";
    app.settings.passport_host = "https://passport-test.yandex.ru";
end

return module;
