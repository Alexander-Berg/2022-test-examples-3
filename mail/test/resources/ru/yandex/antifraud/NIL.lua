local mt = {};
local NIL = {};

function mt.__index()
    return NIL;
end

function mt.__newindex()
    error("NIL is immutable");
end

function mt.__call(self, arg)
    if getmetatable(arg) ~= mt and type(arg) == "table" then
        setmetatable(arg, mt);
        for key, value in pairs(arg) do
            self(value);
        end
    end
    return arg;
end;

function mt.__bor(self, arg)
    return arg;
end;

function mt.__band(self, arg)
    return arg;
end;

setmetatable(NIL, mt);


-- tests
local a = {};
a.b = 1;
a.c = {
    ["d"] = 2
}
NIL(a);

--setmetatable(a, mt);

assert(a.b == 1);
assert(a.c.d == 2);
assert(a.x == NIL);
assert(a.x.y == NIL);

local mutate_ok = pcall(function()
    a.x = 3
end);

assert(not mutate_ok);

return NIL;
