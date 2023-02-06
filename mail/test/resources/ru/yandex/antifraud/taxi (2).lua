local parent = require 'parent'
local luajava = require 'luajava';

local Pattern = luajava.bindClass("java.util.regex.Pattern");

-- -- писать конструкцию, описывающие бизнес значение правила
-- -- не забыть проверить порядок нумерации правил
-- -- тест

local ruleset = {}
--[[
    Title: DEFAULT_r1_wl_countries
    Description: Whitelist'ы стран по БИНу
]]
function ruleset.DEFAULT_r1_wl_countries(context, global)

    local currency = global.src.currency
    if currency == "RUB" and global.src.card_isoa2
        and global.src.src_client_id ~= '0' and global.src.src_client_id ~= 0 and global.src.src_id ~= nil
    then
        if not context.inList('BERU_RU_WL_Countries', global.src.card_isoa2) then
            context.addToQueue('BERU_wl_countries')
            context.comment('r1_wl_countries-RUB')
            return 'DENY'
        end
    end
end

function ruleset.r3_bl_bins(context, global)
    local currency = global.src.currency

    --context.comment(currency .. ' ' .. global.src.src_parent .. tostring(context.inList('TAXI_RU_BL_BINs', "408306")))

    if currency == 'RUB' then
        if context.inList('TAXI_RU_BL_BINs', global.src.src_parent) then
            context.addToQueue('r3_bl_bins')
            context.comment('r3_bl_bins-RUB')
            return 'DENY'
        end
    end
    if currency == 'KZT' then
        if context.inList('TAXI_KZ_BL_BINs', global.src.src_parent) then
            context.addToQueue('r3_bl_bins_kz')
            context.comment('r3_bl_bins-KZT')
            return 'DENY'
        end
    end
    if currency == 'ILS' then
        if context.inList('TAXI_ISR_BL_BINs', global.src.src_parent) then
            context.addToQueue('r3_bl_bins_isr')
            context.comment('r3_bl_bins-ISR')
            return 'DENY'
        end
    end
    if currency == 'RON' then
        if context.inList('TAXI_RO_BL_BINs', global.src.src_parent) then
            context.addToQueue('TAXI_r3_bl_bins_ro')
            context.comment('TAXI_bl_bins_ro')
            return 'DENY'
        end
    end
end

function ruleset.check_aggregates(context, global)
    local aggregates = context.getAggregations()
    assert(aggregates ~= nil)
    assert(aggregates["uid_amnt_m"] == 370000, aggregates["uid_amnt_m"])
end

function ruleset.just_deny(context, global)
    context.addToQueue('just_deny_queue')
    context.comment('just_deny_reason')
    return 'DENY'
end

function ruleset.chec_rbl(context, global)
    assert(global.rep.geoip.country.iso_code == "AM", global.rep.geoip.country.iso_code)
    assert(global.rep.is_yandex_net == false, global.rep.is_yandex_net)
end

function ruleset.chec_rbl(context, global)
    assert(global.rep.geoip.country.iso_code == "AM", global.rep.geoip.country.iso_code)
    assert(global.rep.is_yandex_net == false, global.rep.is_yandex_net)
    assert(context.rbl.infos.geobase.region_info.iso_name == "AM EVN", context.rbl.infos.geobase.region_info.iso_name)
    assert(context.rbl.infos.geobase.ip_traits.is_yandex_net == false, context.rbl.infos.geobase.ip_traits.is_yandex_net)
    assert(context.rbl.infos.geobase.ip_traits.isp_name == "mts armenia cjsc", context.rbl.infos.geobase.ip_traits.isp_name)
end


local PATTERN = Pattern:compile("(?i)abc|cba");
function ruleset.check_re(context)
    assert(PATTERN:matcher("abc"):matches());
    assert(PATTERN:matcher("cba"):matches());
    assert(PATTERN:matcher("Cba"):matches());
    assert(not PATTERN:matcher("abc|cba"):matches());
end

function main(context)
    local global = parent.make_global(context)

    --Вызов функций через pcall.
    --Нужен для корректного скоринга.
    for index, func in pairs(ruleset) do
        local complete, result = pcall(func, context, global)
        if complete then
            if result == 'DENY' then
                context.DENY()
            end
        else
            context.comment(result)
        end
    end

    if global.is_allow then
        return context.ALLOW()
    end
end

App = parent.BaseApp:new()

function App:main(context)
    assert(self.some_field)
    return main(context)
end

function make_app()
    return App:new {
        some_field = true
    }
end
