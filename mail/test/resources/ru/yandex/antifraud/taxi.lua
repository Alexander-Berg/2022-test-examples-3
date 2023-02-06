local parent = require 'parent'

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

local App = parent.BaseApp:new();

function App:main(context)
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

local function make_app()
    return App:new {}
end
