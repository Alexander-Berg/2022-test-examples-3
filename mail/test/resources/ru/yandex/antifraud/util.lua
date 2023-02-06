local module = {};

---@overload fun(value:table|string):number
local function len(value)
    local t = type(value);
    if t == "table" then
        local l = 0;
        for _, _ in pairs(value) do
            l = l + 1;
        end
        return l;
    elseif t == "string" then
        return #value;
    end
    assert(false, "len function applicable to tables and strings, got: " .. t);
end
module.len = len;

---@overload fun(value:any):string
local function any_to_string(value)
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

---@overload fun(v1:any, v2:any):void
local function assert_equals(v1, v2)
    local t1 = type(v1);
    local t2 = type(v2);
    if t1 == t2 and t1 == "table" then
        for k, v in pairs(v1) do
            assert_equals(v, v2[k]);
        end
        for k, v in pairs(v2) do
            assert_equals(v1[k], v);
        end
    else
        assert(v1 == v2, any_to_string(v1) .. " != " .. any_to_string(v2));
    end
end
module.assert_equals = assert_equals;

---@overload fun(n1:number, n2:number):boolean
---@overload fun(n1:number, n2:number, eps:number):boolean
local function numbers_are_equals(n1, n2, eps)
    return math.abs(n1 - n2) < (eps or 1e-5);
end
module.numbers_are_equals = numbers_are_equals;

---@overload fun(v1:number, v2:number):void
local function assert_numbers_are_equals(v1, v2)
    assert(numbers_are_equals(v1, v2), any_to_string(v1) .. " != " .. any_to_string(v2));
end
module.assert_numbers_are_equals = assert_numbers_are_equals;

---@overload fun(table:any):void
local function assert_table(table)
    local t = type(table);
    assert(t == "table", "expected table, got " .. t);
end
module.assert_table = assert_table;

--- Sum iterator for table values
---@overload fun(table:table):function
local function iter_to_table(...)
    local arr = {};
    for v in ... do
        table.insert(arr, v);
    end
    return arr
end
module.iter_to_table = iter_to_table;

--- Sum iterator for table values
---@overload fun(table:table):function
local function values(table)
    assert_table(table);
    local i;
    return function()
        local next_i, value = next(table, i);
        i = next_i;
        return value;
    end
end
module.values = values;

--- Sum iterator for table keys
---@overload fun(table:table):function
local function keys(table)
    assert_table(table);
    local i;
    return function()
        i, _ = next(table, i);
        return i;
    end
end
module.keys = keys;

--- Make new iterator over mapped values from it
---@overload fun(it:function):function
local function map(func, it)
    return function()
        local v = it();
        if v then
            return func(v);
        end
    end
end;
module.map = map;

--- Return true if any value from it is not nil/false
---@overload fun(table:function):function
local function any(it)
    for v in it do
        if v then
            return true;
        end
    end
    return false;
end
module.any = any;

--- Return true if all values from it is not nil/false
---@overload fun(table:function):function
local function all(it)
    for v in it do
        if not v then
            return false;
        end
    end
    return true;
end
module.all = all;

--- Sum iterator values
---@overload fun(it:function):number
local function sum_it(it)
    local s = 0;

    for value in it do
        s = s + value;
    end

    return s;
end
module.sum_it = sum_it;

---@overload fun(dict:table, key:any):number
local function get_probability_dict(dict, key)
    local value = dict[key] or 0;

    if value == 0 then
        return 0;
    end

    local sum = sum_it(map(tonumber, values(dict)));

    if sum ~= 0 then
        return value / sum;
    else
        return 0;
    end
end
module.get_probability_dict = get_probability_dict;

---@overload fun(host:string, level:number):string
local function get_domain(host, level)
    assert(level >= 1, "level must be ge 1");

    local parts = iter_to_table(host:gmatch("[^\\.]+"));
    local size = #parts;

    if level > size then
        return nil
    else
        return table.concat(parts, '.', size - level + 1);
    end
end
module.get_domain = get_domain;

---@overload fun(dict:table, key:any):function
local function recursive_values_by_key(dict, key, result_values)
    assert(key);
    result_values = result_values or {};

    local value = dict[key];

    if value then
        table.insert(result_values, value);
    end

    for v in values(dict) do
        if type(v) == "table" then
            recursive_values_by_key(v, key, result_values);
        end
    end

    return values(result_values);
end
module.recursive_values_by_key = recursive_values_by_key;

local function is_verified_in_services(uid, card_id, t, max_age_in_days, aggregates, needed_verification_level, login_id)
    local one_day = 86400000
    local ver_in_serv = {}
    local was_ver = false
    if login_id and uid and card_id and aggregates.verification_levels then
        for a, vl_list in pairs(aggregates.verification_levels) do
            if vl_list.card_id and vl_list.card_id == card_id and vl_list.uid and vl_list.uid == uid and vl_list.afs_verification_level and vl_list.afs_verification_level == needed_verification_level and vl_list.txn_status and vl_list.txn_status == "OK" and vl_list.login_id and vl_list.login_id == login_id then
                age = t - vl_list["last_acquire"]
                if age / one_day < max_age_in_days then
                    ver_in_serv[vl_list["service_id"]] = true;
                    was_ver = true
                end
            end
        end
    end

    return was_ver, ver_in_serv
end
module.is_verified_in_services = is_verified_in_services;

---@overload fun(dict:table, except_key:string):table
local function copy_except(dict, except_key)
    assert(dict);
    assert(except_key);

    local copy = {};

    for key, value in pairs(dict) do
        if key ~= except_key then
            if value and type(value) == "table" then
                value = copy_except(value, except_key);
            end
            copy[key] = value;
        end
    end

    return copy;
end
module.copy_except = copy_except;

assert_equals(len({ ["a"] = 5, 3 }), 2);
assert_equals(len("test"), 4);

assert_equals(any_to_string({ ["a"] = 5, "c" }), '{1:"c","a":5}');

assert_equals(get_domain("luckybug.yandex.ru", 1), "ru");
assert_equals(get_domain("luckybug.yandex.ru", 2), "yandex.ru");
assert_equals(get_domain("luckybug.yandex.ru", 3), "luckybug.yandex.ru");
assert_equals(get_domain("luckybug.yandex.ru", 4), nil);

assert_equals(sum_it(keys({ "1", 2, 4 })), 6);
assert_equals(sum_it(values({ "1", 2, 4 })), 7);

assert_numbers_are_equals(get_probability_dict({ ["a"] = 5, ["b"] = 3 }, "a"), 5 / 8);
assert_numbers_are_equals(get_probability_dict({ ["a"] = 5, ["b"] = 3 }, "b"), 3 / 8);
assert_numbers_are_equals(get_probability_dict({ ["a"] = 5, ["b"] = 3 }, "c"), 0);
assert_numbers_are_equals(get_probability_dict({}, "c"), 0);

assert_equals(iter_to_table(recursive_values_by_key({ ["k"] = 123 }, "k")), { 123 });
assert_equals(iter_to_table(recursive_values_by_key({ ["k"] = 123, ["sub"] = { ["k"] = 456 }, ["l"] = 789 }, "k")), { 123, 456 });
assert_equals(iter_to_table(recursive_values_by_key({ ["k"] = 123, ["sub"] = { ["k"] = 456 }, ["l"] = 789 }, "l")), { 789 });

assert_equals(sum_it(recursive_values_by_key({ ["k"] = 123, ["sub"] = { ["k"] = 456 }, ["l"] = 789 }, "k")), 123 + 456);
assert_equals(sum_it(recursive_values_by_key({ ["k"] = 123, ["sub"] = { ["k"] = 456 }, ["l"] = 789 }, "l")), 789);

assert_equals(iter_to_table(map(function(v)
    return v * v;
end, values({ 1, 2, 3 }))), { 1, 4, 9 });

assert_equals(all(values({ 1, "asd", {} })), true);
assert_equals(all(values({ 1, "asd", {}, false })), false);
assert_equals(all(values({})), true);

assert_equals(any(values({ 1, "asd", {} })), true);
assert_equals(any(values({ 1, "asd", {}, false })), true);
assert_equals(any(values({ false })), false);
assert_equals(any(values({})), false);

assert_equals(copy_except({ a = 1, b = { c = 2 } }, "c"), { a = 1, b = { } });

return module;
