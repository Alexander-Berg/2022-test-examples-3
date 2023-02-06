-- Блок внутренних функций политики

local module = {}

function module.make_global(context)

    local global = {}
    -- --Блок внутренних функций Системы
    global.src = context.getSource()
    --global.event = getEvent()
    global.aggrs = context.getAggregations()
    global.rep = context.getIpReputation()
    --global.tel = getTelemetry()
    --global.consts = getConstants()
    global.is_allow = false
    function global.set_allow()
        if not global.is_allow then
            global.is_allow = true
        end
        return global.is_allow
    end

    function global.len(t)
        count = 0
        for k, v in pairs(t) do
            count = count + 1
        end
        return count
    end

    function global.addToSet(set, key)
        set[key] = true
    end

    function global.removeFromSet(set, key)
        set[key] = nil
    end

    function global.setContains(set, key)
        return set[key] ~= nil
    end

    function global.total_count(a)
        local uniq = {}
        for _, v in pairs(a)
        do
            if not string.match(v, "_tips") then
                global.addToSet(uniq, v)
            end
        end
        return uniq
    end
    function global.aggrs_count(a, b)
        if b == nil then
            b = 0
        end
        for _, v in pairs(a)
        do
            if v then
                b = b + 1
            end
        end
        return b
    end

    global.dict_checkbox = false
    function global.dict_check()
        if not global.dict_checkbox then
            global.dict_checkbox = true
        end
        return global.dict_checkbox
    end

    global.consts = {}
    global.consts.card_amnt_h_rub = 10
    global.consts.card_amnt_d_rub = 10
    global.consts.card_amnt_w_rub = 10
    global.consts.card_foreign_ip_tnx_cnt_h_rub = 0
    global.consts.card_foreign_ip_tnx_cnt_d_rub = 0
    global.consts.card_tnx_cnt_h_rub = 0
    global.consts.card_tnx_cnt_d_rub = 0
    global.consts.card_tnx_cnt_w_rub = 0
    global.consts.card_uids_cnt_h_rub = 0
    global.consts.card_uids_cnt_h_rub_preprod = 0
    global.consts.card_uids_cnt_d_rub = 0
    global.consts.card_uids_cnt_d_rub_preprod = 0
    global.consts.card_uids_cnt_w_rub = 0
    global.consts.foreign_card_uids_cnt_h_rub = 0
    global.consts.foreign_card_uids_cnt_d_rub = 0
    global.consts.foreign_card_uids_cnt_w_rub = 0
    global.consts.limit_min_amnt_for_huge_purchase_w = 0
    global.consts.limit_min_tn_cnt_for_huge_purchase_w = 0
    global.consts.limit_card_failed_cnt_h = 0
    global.consts.limit_card_failed_cnt_h_preprod = 0
    global.consts.limit_card_failed_cnt_d = 0
    global.consts.limit_card_failed_cnt_d_preprod = 0
    global.consts.limit_card_failed_cnt_w = 0
    global.consts.limit_card_failed_cnt_m = 0
    global.consts.limit_count_ip_failed_hour = 0
    global.consts.limit_count_ip_failed_day = 0
    global.consts.limit_count_uid_ip_hour = 0
    global.consts.limit_count_uid_ip_day = 0
    global.consts.limit_uid_failed_cnt_h = 0
    global.consts.limit_uid_failed_cnt_h_preprod = 0
    global.consts.limit_uid_failed_cnt_d = 0
    global.consts.limit_uid_failed_cnt_d_preprod = 0
    global.consts.limit_uid_failed_cnt_w = 0
    global.consts.limit_uid_failed_cnt_m = 0
    global.consts.mail_amnt_h_rub = 0
    global.consts.mail_amnt_d_rub = 0
    global.consts.mail_amnt_w_rub = 0
    global.consts.mail_tnx_cnt_h_rub = 0
    global.consts.mail_tnx_cnt_d_rub = 0
    global.consts.mail_tnx_cnt_w_rub = 0
    global.consts.min_amnt_for_uid_faild_h = 0
    global.consts.tnx_max_amount_rub = 0
    global.consts.uid_amnt_h_rub = 0
    global.consts.uid_amnt_d_rub = 0
    global.consts.uid_amnt_w_rub = 0
    global.consts.uid_cards_cnt_h_rub = 0
    global.consts.uid_cards_cnt_d_rub = 0
    global.consts.uid_cards_cnt_d_rub_preprod = 0
    global.consts.uid_cards_cnt_w_rub = 0
    global.consts.uid_cards_cnt_w_rub_preprod = 0
    global.consts.uid_foreign_cards_cnt_h_rub = 0
    global.consts.uid_foreign_cards_cnt_d_rub = 0
    global.consts.uid_foreign_cards_cnt_w_rub = 0
    global.consts.uid_foreign_ips_cnt_h_rub = 0
    global.consts.uid_foreign_ips_cnt_d_rub = 0
    global.consts.uid_tnx_cnt_h_rub = 0
    global.consts.uid_tnx_cnt_d_rub = 0
    global.consts.uid_tnx_cnt_w_rub = 0

    return global
end

local BaseApp = {}

function BaseApp:new (o)
    o = o or {}
    setmetatable(o, self)
    self.__index = self
    return o
end

function BaseApp:prepare(prepare_context)
end

function BaseApp:main(main_context)
end

module.BaseApp = BaseApp;

local BaseSaveApp = {}

function BaseSaveApp:new (o)
    o = o or {}
    setmetatable(o, self)
    self.__index = self
    return o
end

function BaseSaveApp:prepare(prepare_context)
end

function BaseSaveApp:main(main_context)
end

module.BaseSaveApp = BaseSaveApp;

function any_to_string(value)
    local t = type(value);
    if "table" == t then
        local result = '{';
        local first = true;
        for key, v in pairs(value) do
            if not first then
                result = result .. ',';
            end

            result = result .. any_to_string(key) .. ':' .. any_to_string(v);

            first = false;
        end
        return result .. '}';
    elseif "string" == t then
        return '"' .. value .. '"'
    else
        return tostring(value);
    end
end
module.any_to_string = any_to_string;

return module
