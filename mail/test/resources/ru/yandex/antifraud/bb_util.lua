local time_constants = require('time_constants');
local libphonenumber = require('libphonenumber');
local util = require('util');

local module = {};

local ATTRIBUTES = {
    ACCOUNT_REGISTRATION_DATETIME = "1",
    ACCOUNT_IS_MAILLIST = "13",
    PERSON_COUNTRY = "31",
    PHONE_CONFIRMATION_TIMESTAMP = "36",
    IS_APP_PASSWORD_ENABLED = "107",
    PHONES_SECURE = "110",
    IS_SHARED = "132",
    ACCOUNT_SMS_2FA_ON = "200",
    ACCOUNT_2FA_ON = "1003",
    ACCOUNT_HAVE_PLUS = "1015",
    ACCOUNT_CONNECT_ORGANIZATION_IDS = "1017",
};
module.ATTRIBUTES = ATTRIBUTES;

local ALIASES = {
    PORTAL = "1",
    MAIL = "2",
    NAROD_MAIL = "3",
    LITE = "5",
    SOCIAL = "6",
    PDD = "7",
    PDDALIAS = "8",
    ALTDOMAIN = "9",
    PHONISH = "10",
    PHONENUMBER = "11",
    MAILISH = "12",
    YANDEXOID = "13",
    KINOPOISK_ID = "15",
    UBER_ID = "16",
    YAM_BOT = "17",
    KOLONKISH = "18",
    PUBLIC_ID = "19",
    OLD_PUBLIC_ID = "20",
    NEOPHONISH = "21",
    KIDDISH = "22",
};
module.ALIASES = ALIASES;

local PHONE_ATTRIBUTES = {
    PHONE_NUMBER = "101",
    PHONE_E164_NUMBER = "102",
    PHONE_MASKED_E164_NUMBER = "104",
    IS_BOUND = "106",
    IS_SECURED = "108",
};
module.PHONE_ATTRIBUTES = PHONE_ATTRIBUTES;

function module.get_registration_datetime_seconds(bb)
    return bb and bb.attributes and tonumber(bb.attributes[ATTRIBUTES.ACCOUNT_REGISTRATION_DATETIME]);
end

function module.get_uid_age(bb, now)
    if not bb then
        return ;
    end
    local uid_regdate_ts = module.get_registration_datetime_seconds(bb);
    if not uid_regdate_ts then
        return ;
    end
    local cur_ts = now or os.time() * time_constants.SECOND;
    return (cur_ts - uid_regdate_ts * time_constants.SECOND);
end

function module.get_uid_age_in_days(context)
    local age = module.get_uid_age(context.raw_bb, context.src and context.src.t);
    return age and age / time_constants.DAY;
end

function module.get_uid_age_in_minutes(context)
    local age = module.get_uid_age(context.raw_bb, context.src and context.src.t);
    return age and age / time_constants.MINUTE;
end

function module.get_karma(bb)
    return bb and bb.karma and bb.karma.value;
end

function module.get_karma_status(bb)
    return bb and bb.karma_status and bb.karma_status.value;
end

function module.get_display_name(bb)
    return bb and bb.display_name and bb.display_name.name;
end

function module.has_password(bb)
    return bb and bb.have_password;
end

function module.is_account_2fa_on(bb)
    return bb and bb.attributes and bb.attributes[ATTRIBUTES.ACCOUNT_2FA_ON];
end

function module.is_account_sms_2fa_on(bb)
    return bb and bb.attributes and bb.attributes[ATTRIBUTES.ACCOUNT_SMS_2FA_ON];
end

function module.is_shared(bb)
    return bb and bb.attributes and bb.attributes[ATTRIBUTES.IS_SHARED];
end

function module.get_person_country(bb)
    return bb and bb.attributes and bb.attributes[ATTRIBUTES.PERSON_COUNTRY];
end

function module.account_has_plus(bb)
    return bb and bb.attributes and bb.attributes[ATTRIBUTES.ACCOUNT_HAVE_PLUS];
end

function module.get_login(bb)
    return bb and bb.login ~= "" and bb.login or nil;
end

function module.is_any_phone_has_attribute(bb, attribute_id)
    local phones = bb and bb.phones or {};
    for _, phone in pairs(phones) do
        local attribute = phone.attributes[attribute_id];
        if attribute then
            return attribute;
        end
    end
    return nil;
end

function module.is_any_phone_bound(bb)
    return module.is_any_phone_has_attribute(bb, PHONE_ATTRIBUTES.IS_BOUND);
end

function module.is_any_phone_secured(bb)
    return module.is_any_phone_has_attribute(bb, PHONE_ATTRIBUTES.IS_SECURED);
end

function module.is_any_phone_e164(bb)
    return module.is_any_phone_has_attribute(bb, PHONE_ATTRIBUTES.PHONE_E164_NUMBER);
end

function module.is_alias_lite_only(bb)
    local aliases = bb and bb.aliases or {};
    local is_lite = false;
    for alias, value in pairs(aliases) do
        if alias == ALIASES.LITE and value then
            is_lite = true;
        else
            return false
        end
    end
    return is_lite;
end

assert(not module.is_alias_lite_only({
    aliases = {}
}));

assert(module.is_alias_lite_only({
    aliases = {
        [ALIASES.LITE] = "1",
    }
}));

assert(not module.is_alias_lite_only({
    aliases = {
        [ALIASES.LITE] = "1",
        [ALIASES.PORTAL] = "1",
    }
}));

function module.is_alias_lite(bb)
    local aliases = bb and bb.aliases or {};
    return aliases[ALIASES.LITE];
end

function module.is_alias_portal(bb)
    local aliases = bb and bb.aliases or {};
    return aliases[ALIASES.PORTAL];
end

function module.is_alias_neophonish(bb)
    local aliases = bb and bb.aliases or {};
    return aliases[ALIASES.NEOPHONISH];
end

local function IGNORE_ERROR()
end

--https://st.yandex-team.ru/PASSP-34733#617942cffa9470235e5e5f7e
function module.robust_login(bb)
    local phone = module.is_any_phone_e164(bb);
    local display_name = module.get_display_name(bb);
    local login = module.get_login(bb);

    local e614_phone, national_number;
    if phone then
        _, e614_phone, national_number = xpcall(libphonenumber.e164, IGNORE_ERROR, phone);
    end

    local e164_display_name;
    if display_name then
        _, e164_display_name = display_name and xpcall(libphonenumber.e164, IGNORE_ERROR, display_name);
    end

    if display_name and national_number and module.is_alias_neophonish(bb) and not module.is_alias_portal(bb) then
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

local bb_neophonish_mock = {
    ["aliases"] = {
        ["21"] = "nphne-m3bqq6gt"
    },
    ["attributes"] = {
        ["1"] = "1627903087",
        ["31"] = "ru",
        ["110"] = "540064656"
    },
    ["display_name"] = {
        ["avatar"] = {
            ["default"] = "0/0-0",
            ["empty"] = true
        },
        ["name"] = "alina kit"
    },
    ["family_info"] = {},
    ["have_hint"] = false,
    ["have_password"] = false,
    ["id"] = "1460669987",
    ["karma"] = {
        ["value"] = 0
    },
    ["karma_status"] = {
        ["value"] = 0
    },
    ["login"] = "",
    ["phones"] = {
        {
            ["attributes"] = {
                ["102"] = "+79251234566",
                ["104"] = "+7925*****66",
                ["106"] = "1",
                ["108"] = "1"
            },
            ["id"] = "540064656"
        }
    },
    ["regname"] = "nphne-m3bqq6gt",
    ["uid"] = {
        ["hosted"] = false,
        ["lite"] = false,
        ["value"] = "1460669987"
    }

}

util.assert_equals("+7 (925) 123-45-66 (alina kit)", module.robust_login(bb_neophonish_mock));

return module;
