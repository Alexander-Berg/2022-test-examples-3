local parent = require 'parent'
local yasm = require 'yasm'
local solomon = require 'solomon'
local math = require 'math'
local uatraits = require 'uatraits'
local luajava = require 'luajava'
local traffic_features = require 'traffic_features';
local currency = require('currency');
local passport = require('passport')
local bb = require('bb')
local trust = require('trust')
local pharma = require('pharma')
local libphonenumber = require('libphonenumber')
local bb_util = require 'bb_util';
local json = require 'json';
local NIL = require 'NIL';
local util = require 'util';

local IDN = luajava.bindClass("java.net.IDN");
local Instant = luajava.bindClass("java.time.Instant");
local LocalDateTime = luajava.bindClass("java.time.LocalDateTime");
local ZoneId = luajava.bindClass("java.time.ZoneId");

local wl_countries_stat = yasm.register_signal("wl_countries")
local some_stat = solomon.rate("wl_countries", { ["label_name"] = "label_value" })

local ruleset = {}

function ruleset.check_logout(context)
    passport.logout(context, "logout luckybug", "luckybug");
    passport.change_password(context, "change_password lomanovvasiliy", "luckybug", 5, false, true)
end

--[[
https://wiki.yandex-team.ru/passport/api/bundle/auth/challenge/#otpravkaemailiopcionalnopushposleavtorizacii
]]
function ruleset.send_mail(context)
    passport.send_mail(context, false)
end

function ruleset.test_client(context)
    --@Nonnull String clientName,
    --@Nonnull String path,
    --@Nonnull LuaValue data,
    --@Nonnull LuaFunction callback

    assert(context.cache_put_response == 123, '<' .. context.cache_put_response .. '>');
    assert(context.cache_get_response.key == "value", context.cache_get_response);
end

function ruleset.send_custom_push(context)
    passport.send_custom_push {
        context = context,
        --required
        uid = 1120000000036393,
        push_service = "push_name",
        event_name = "some_event_name",
        title = "some title",
        --not required
        check_subscriptions = false,
        body = "some body",
        subtitle = "some subtitle",
        webview_url = "localhost",
        require_web_auth = true,
        zoo_hash = "1234567" }
end

function ruleset.check_pharma(context)
    assert(context.pharma.factors.usages.last_year == 1)
end

function ruleset.check_lists(context)
    assert(context.inLocalList("TAXI_RU_BL_BINs", "462239"))
    assert(context.inLocalList("beru", "payment", "TAXI_RU_BL_BINs", "462239"))

    context.putInList("UIDS_LIST", 338926190, "author", "some meaningfull reason", 365)
end

function ruleset.check_bb(context)
    local is_bound = context.raw_bb.phones[1].attributes["106"]
    assert(is_bound)
end

function ruleset.check_models(context)
    local numericFeatures = {
        ["syn_quirk_nz_ack"] = 1
    }
    local categoricalFeatures = {
    }
    local result = context.calcModel("frodo", numericFeatures, categoricalFeatures)
    assert(math.abs(result[1] - 7.005121) < 1e-5, result[1])
end

function numbers_are_equals(n1, n2, eps)
    return math.abs(n1 - n2) < (eps or 1e-5);
end

function ruleset.check_traffic_model(context)
    assert(context.src ~= null)
    assert(context.src.traffic_fp ~= null)
    local categoricalFeatures = {
    }

    local lua_features = traffic_features.make_features(context.src.traffic_fp);

    local result = context.calcModel("frodo", lua_features, categoricalFeatures)
    assert(numbers_are_equals(result[1], -3.1784256), result[1])
end

function ruleset.check_log(context)
    context.log("log_key_1", { ["key"] = "value" });
    context.log("log_key_2", 42);
end

function ruleset.check_uatraits()
    local parsed = uatraits.parse("Mozilla/5.0 (X11; Ubuntu; Linux x86_64; rv:89.0) Gecko/20100101 Firefox/89.0")

    assert("Firefox" == parsed.BrowserName);
    assert("Gecko" == parsed.BrowserEngine);
    assert("true" == parsed.x64);
    assert("Ubuntu" == parsed.OSName);
    assert("false" == parsed.isTouch);
    assert("false" == parsed.isMobile);
    assert("89.0" == parsed.BrowserVersion);
    assert("89.0" == parsed.BrowserEngineVersion);
    assert("true" == parsed.isBrowser);
end

function ruleset.check_trust(context)
    --{
    --    "balances": [
    --    {
    --        "wallet_id": "w/1da24139-7674-5f06-9905-e0f494194d3a",
    --        "amount": "0",
    --        "currency": "RUB"
    --    }
    --    ]
    --}

    local balance = context.trust.balances[1]
    assert(balance.wallet_id == "w/1da24139-7674-5f06-9905-e0f494194d3a")
    assert(balance.amount == "0")
    assert(balance.currency == "RUB")

end

function iter_to_table(...)
    local arr = {}
    local len = 0
    for v in ... do
        arr[#arr + 1] = v
        len = len + 1
    end
    return arr, len
end

function get_domain(host, level)
    local parts, size = iter_to_table(host:gmatch("[^\\.]+"))

    if level > size then
        return nil
    else
        return parts[size - level + 1]
    end
end

function get_probability(dict, key)
    local sum = 0;
    local value = dict[key] or 0;

    if value == 0 then
        return 0;
    end

    for _, v in pairs(dict) do
        sum = sum + v;
    end

    if sum ~= 0 then
        return value / sum;
    else
        return 0;
    end
end

function ruleset.make_features(context)
    local src = context.src;

    local cat_features = {};
    local num_features = {};

    cat_features["ip"] = src.ip or "";

    if src.yandexuid_timestamp then
        num_features["yandexuid_timestamp"] = src.yandexuid_timestamp;
        num_features["yandexuid_ts_freshness"] = src.t / 1000 - src.yandexuid_timestamp;
        num_features["is_yandexuid_ts_future"] = src.t / 1000 < src.yandexuid_timestamp and 1 or 0;
    else
        num_features["yandexuid_timestamp"] = -1;
        num_features["yandexuid_ts_freshness"] = -1;
        num_features["is_yandexuid_ts_future"] = -1;
    end

    local region_id = "";
    local country_id = "";
    local city_id = "";
    local as = "";
    if context.rbl then
        local geobase = context.rbl.infos.geobase;
        local region_info = geobase.region_info;
        local asn_list = geobase.ip_traits.asn_list or {};

        region_id = region_info.id or "";
        country_id = region_info.country_id or "";
        city_id = region_info.city_id or "";
        as = asn_list[1] or {};
    end

    cat_features["region_id"] = region_id
    cat_features["country_id"] = country_id
    cat_features["city_id"] = city_id
    cat_features["as"] = as

    if src.retpath then
        local url = luajava.newInstance("java.net.URL", src.retpath);
        cat_features["retpath_host_2"] = IDN.toUnicode(get_domain(url:getHost(), 2) or "");
        cat_features["retpath_host_3"] = IDN.toUnicode(get_domain(url:getHost(), 3) or "");
    else
        cat_features["retpath_host_2"] = "";
        cat_features["retpath_host_3"] = "";
    end

    local now = LocalDateTime:ofInstant(Instant:ofEpochMilli(src.t), ZoneId:systemDefault())

    num_features["day_part"] = now:getHour() / 6;
    num_features["weekday"] = now:getDayOfWeek():getValue();
    num_features["hour"] = now:getHour();
    num_features["month"] = now:getMonthValue();

    local os_name = "";
    local browser_name = "";
    local os_family = "";
    local os_version = "";
    local browser_version = "";
    if src.user_agent then
        local parts = uatraits.parse(src.user_agent);

        num_features["is_mobile"] = parts.isMobile == "true" and 1 or 0;
        os_name = parts.OSName;
        browser_name = parts.BrowserName;
        os_family = parts.OSFamily;
        os_version = parts.OSVersion;
        browser_version = parts.BrowserVersion;
    end

    local browser = browser_name .. ' ' .. browser_version;
    local browser_os = browser_name .. ' ' .. browser_version .. " - " .. os_name;

    num_features["browser_prob_6m"] = get_probability(src.browser_freq or {}, browser);
    num_features["browser_name_prob_6m"] = get_probability(src.browser_name_freq or {}, browser_name);
    num_features["browser_os_prob_6m"] = get_probability(src.browser_os_freq or {}, browser_os);
    num_features["os_name_prob_6m"] = get_probability(src.os_name_freq or {}, os_name);
    num_features["os_family_prob_6m"] = get_probability(src.os_family_freq or {}, os_family);
    num_features["ip_prob_6m"] = get_probability(src.ip_freq or {}, src.ip);
    num_features["city_id_prob_6m"] = get_probability(src.city_freq or {}, city_id);
    num_features["country_id_prob_6m"] = get_probability(src.country_freq or {}, country_id);
    num_features["as_list_prob_6m"] = get_probability(src.as_list_freq or {}, as);
    num_features["su_ip_prob_6m"] = get_probability(src.su_ip_freq or {}, src.ip);
    num_features["su_ip_prob_6m"] = get_probability(src.su_ip_freq or {}, src.ip);
    num_features["su_country_prob_6m"] = get_probability(src.su_country_freq or {}, country_id);
    num_features["su_city_prob_6m"] = get_probability(src.su_city_freq or {}, city_id);
    num_features["su_as_list_prob_6m"] = get_probability(src.su_as_list_freq or {}, as);
    num_features["it_as_list_prob_6m"] = get_probability(src.it_as_list_freq or {}, as);
    num_features["it_city_prob_6m"] = get_probability(src.it_city_freq or {}, city_id);
    num_features["it_country_id_prob_6m"] = get_probability(src.it_country_freq or {}, country_id);

    num_features["ip_bind_cards_cnt_m"] = context.aggrs["ip_bind_cards_cnt_m"] or 0;
    num_features["ip_bind_tnx_cnt_m"] = context.aggrs["ip_bind_tnx_cnt_m"] or 0;
    num_features["ip_cards_cnt_m"] = context.aggrs["ip_cards_cnt_m"] or 0;
    num_features["ip_currencies_cnt_m"] = context.aggrs["ip_currencies_cnt_m"] or 0;
    num_features["ip_device_ids_m_set"] = context.aggrs["ip_device_ids_m_set"] or 0;
    num_features["ip_failed_bind_cards_cnt_m"] = context.aggrs["ip_failed_bind_cards_cnt_m"] or 0;
    num_features["ip_failed_bind_tnx_cnt_m"] = context.aggrs["ip_failed_bind_tnx_cnt_m"] or 0;
    num_features["ip_failed_cnt_m"] = context.aggrs["ip_failed_cnt_m"] or 0;
    num_features["ip_failed_device_ids_cnt_m"] = context.aggrs["ip_failed_device_ids_cnt_m"] or 0;
    num_features["ip_failed_tnx_cnt_m"] = context.aggrs["ip_failed_tnx_cnt_m"] or 0;
    num_features["ip_foreign_cards_cnt_m"] = context.aggrs["ip_foreign_cards_cnt_m"] or 0;
    num_features["ip_foreign_uids_cnt_m"] = context.aggrs["ip_foreign_uids_cnt_m"] or 0;
    num_features["ip_tnx_cnt_m"] = context.aggrs["ip_tnx_cnt_m"] or 0;
    num_features["ip_uids_cnt_m"] = context.aggrs["ip_uids_cnt_m"] or 0;
    num_features["mail_countries_cnt_m"] = context.aggrs["mail_countries_cnt_m"] or 0;
    num_features["mail_ips_cnt_m"] = context.aggrs["mail_ips_cnt_m"] or 0;
    num_features["uid_3ds_tnx_cnt_m"] = context.aggrs["uid_3ds_tnx_cnt_m"] or 0;
    num_features["uid_amnt_m"] = context.aggrs["uid_amnt_m"] or 0;
    num_features["uid_bin_countries_cnt_m"] = context.aggrs["uid_bin_countries_cnt_m"] or 0;
    num_features["uid_bind_cards_cnt_m"] = context.aggrs["uid_bind_cards_cnt_m"] or 0;
    num_features["uid_bind_tnx_cnt_m"] = context.aggrs["uid_bind_tnx_cnt_m"] or 0;
    num_features["uid_cards_cnt_m"] = context.aggrs["uid_cards_cnt_m"] or 0;
    num_features["uid_countries_cnt_m"] = context.aggrs["uid_countries_cnt_m"] or 0;
    num_features["uid_currencies_cnt_m"] = context.aggrs["uid_currencies_cnt_m"] or 0;
    num_features["uid_device_ids_cnt_m"] = context.aggrs["uid_device_ids_cnt_m"] or 0;
    num_features["uid_failed_bind_cards_cnt_m"] = context.aggrs["uid_failed_bind_cards_cnt_m"] or 0;
    num_features["uid_failed_bind_tnx_cnt"] = context.aggrs["uid_failed_bind_tnx_cnt"] or 0;
    num_features["uid_failed_cnt_m"] = context.aggrs["uid_failed_cnt_m"] or 0;
    num_features["uid_failed_countries_cnt_m"] = context.aggrs["uid_failed_countries_cnt_m"] or 0;
    num_features["uid_failed_device_ids_cnt_m"] = context.aggrs["uid_failed_device_ids_cnt_m"] or 0;
    num_features["uid_failed_tnx_cnt_m"] = context.aggrs["uid_failed_tnx_cnt_m"] or 0;
    num_features["uid_foreign_cards_cnt_m"] = context.aggrs["uid_foreign_cards_cnt_m"] or 0;
    num_features["uid_foreign_ip_tnx_cnt_m"] = context.aggrs["uid_foreign_ip_tnx_cnt_m"] or 0;
    num_features["uid_foreign_ips_cnt_m"] = context.aggrs["uid_foreign_ips_cnt_m"] or 0;
    num_features["uid_ips_cnt_m"] = context.aggrs["uid_ips_cnt_m"] or 0;
    num_features["uid_plus_topup_amount_m"] = context.aggrs["uid_plus_topup_amount_m"] or 0;
    num_features["uid_plus_withdraw_amount_m"] = context.aggrs["uid_plus_withdraw_amount_m"] or 0;
    num_features["uid_plus_withdraw_tnx_count_m"] = context.aggrs["uid_plus_withdraw_tnx_count_m"] or 0;
    num_features["uid_regions_cnt_m"] = context.aggrs["uid_regions_cnt_m"] or 0;
    num_features["uid_succ_3ds_tnx_cnt_m"] = context.aggrs["uid_succ_3ds_tnx_cnt_m"] or 0;
    num_features["uid_succ_amnts_sum_m"] = context.aggrs["uid_succ_amnts_sum_m"] or 0;
    num_features["uid_succ_cards_cnt_m"] = context.aggrs["uid_succ_cards_cnt_m"] or 0;
    num_features["uid_succ_countries_cnt_m"] = context.aggrs["uid_succ_countries_cnt_m"] or 0;
    num_features["uid_succ_device_ids_cnt_m"] = context.aggrs["uid_succ_device_ids_cnt_m"] or 0;
    num_features["uid_succ_tnx_cnt_m"] = context.aggrs["uid_succ_tnx_cnt_m"] or 0;
    num_features["uid_tnx_cnt_m"] = context.aggrs["uid_tnx_cnt_m"] or 0;
    num_features["uid_yids_cnt_m"] = context.aggrs["uid_yids_cnt_m"] or 0;

    local result = context.calcModel("frodo", num_features, cat_features)
    assert(math.abs(result[1] - 7.005121) < 1e-5, result[1])

    print("num_features: " .. parent.any_to_string(num_features))
    print("cat_features: " .. parent.any_to_string(cat_features))
end

function ruleset.check_prepared_counters(context)
    local checked_counter = context.counterToCheck:checked();

    assert(checked_counter.first_acquire == 1593536181134)
    assert(checked_counter.last_acquire == 1693536181134)
    assert(checked_counter.key == "some_key");

    assert(checked_counter.data.some_key == "some value");

    context.counterToCheck:touch();
end

function ruleset.create_track(context)
    local response = context.create_track_response;

    assert(response);
    assert(response.track_id == "abcdefghabcdefgh");
end

function ruleset.check_currencies_rates(context)
    local rate = currency.rate("RUB", "USD");
    assert(rate == 1 / 72.8491, rate);

    rate = currency.rate("USD", "RUB");
    assert(rate == 72.8491, rate);

    rate = currency.to_rub_rate("USD");
    assert(rate == 72.8491, rate);

    assert(context.nsrc.rub_amount == 10331, context.nsrc.rub_amount);
end

function ruleset.test_3164()
    local e164 = libphonenumber.e164(89257197019);
    assert("+79257197019" == e164, e164)

    local e164, international = libphonenumber.e164(89257197019);
    assert("+79257197019" == e164, e164)
    assert("+7 (925) 719-70-19" == international, international);

    local parsed = libphonenumber.parse(89257197019);
    assert(parsed:formatE164() == e164, parsed:formatE164());
    assert(parsed:formatInternational() == international, parsed:formatInternational());
    assert(parsed:regionCodeForNumber() == "RU", parsed:regionCodeForNumber());

end

function ruleset.test_custom(context)
    context.custom("custom_key", {
        custom_sub_key = "custom_value"
    });
end

local function robust_login(bb)
    local phone = bb_util.is_any_phone_e164(bb);
    local display_name = bb_util.get_display_name(bb);
    local login = bb_util.get_login(bb);

    local function IGNORE()
    end
    local _, e614_phone, national_number = phone and xpcall(libphonenumber.e164, IGNORE, phone);
    local _, e164_display_name = display_name and xpcall(libphonenumber.e164, IGNORE, display_name);

    if display_name and national_number and bb_util.is_alias_neophonish(bb) and not bb_util.is_alias_portal(bb) then
        if e614_phone ~= e164_display_name then
            return national_number .. ' (' .. display_name .. ')';
        else
            return national_number;
        end
    elseif login then
        return login;
    else
        return display_name;
    end
end

function ruleset.test_robust_login(context)
    local login = robust_login(context.raw_bb);
    assert(login == "LomanovVasiliy", login);
end

function ruleset.test_templates()
    local template = json.load("PushNotification.json");
    assert(template.az.PushNotification["family-pay-push"].action.disable[1] == "deaktiv etdi");
end

App = parent.BaseApp:new()

function App:post_action_impl(context)
    assert(context.resolution() == "ALLOW");
end

function App:prepare(context)
    context.settings = {};
    passport.setup_prod(context);
    self.valueToCheck1 = context.toCheckInList("BLACK_UIDS", "1120000000036393")
    self.valueToCheck2 = context.toCheckInList("PREPARED_FAST_LIST", "1120000000036393")
    self.valueToCheck3 = context.toCheckInList("PREPARED_FAST_LIST", "123")
    self.valueToCheck4 = context.toCheckInList("PREPARED_FAST_LIST", 123)

    context.counterToCheck = context.counters:counterToCheck("some_key", "value");

    context.counterToCheckEscaping = context.counters:counterToCheck("user_agent", "Mozilla/5.0 (Macintosh; Intel Mac OS X 10_15_7) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/90.0.4430.41 YaBrowser/21.5.0.751 Yowser/2.5 Safari/537.36");

    passport.create_track(context, function(err, response)
        context.create_track_response = response;
    end);

    local query = context.client:query("/put", { key = "some-key", ttl = "3h" });
    local data = context.client:json({ key = "value" });

    context.client:post("CacheClient", query, data, function(err, response)
        context.cache_put_response = response;
    end);

    context.client:get("CacheClient", context.client:query("/get", { key = "some-key" }),
        function(err, response)
            context.cache_get_response = response;
        end);

    context.client:get("CacheClient", context.client:query("/get", { key = "another-key" }),
        function(err, response)
            assert(err);
            assert(not response);
        end);

    bb.userinfo(context, context.nsrc.uid, function(err, response)
        assert(not err);
        context.raw_bb = response;
    end);

    trust.wallet_balance(context, context.nsrc.uid, function(err, response)
        assert(not err);
        context.trust = response;
    end);

    pharma.phone_number_factors(context, context.nsrc.user_phone, function(err, response)
        assert(not err);
        context.pharma = response;
    end);
end

function App:main(context)
    assert(self.valueToCheck1:isInList() == false)
    assert(self.valueToCheck2:isInList() == false)
    assert(self.valueToCheck3:isInList() == true)
    assert(self.valueToCheck4:isInList() == true)

    local global = parent.make_global(context)

    some_stat:push(1)
    -- -- писать конструкцию, описывающие бизнес значение правила
    -- -- не забыть проверить порядок нумерации правил
    -- -- тест


    --Вызов функций через pcall.
    --Нужен для корректного скоринга.
    for _, func in pairs(ruleset) do
        local complete, result = pcall(func, context, global)
        if complete then
            if result == 'DENY' then
                context.DENY()
            end
        else
            context.comment("lua error:" .. result)
        end
    end

    if global.is_allow then
        return context.ALLOW()
    end
end

function make_app()
    return App:new {}
end
