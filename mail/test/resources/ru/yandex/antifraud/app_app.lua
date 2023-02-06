local App = {};

function App:new (o)
    o = o or {}
    setmetatable(o, self)
    self.__index = self
    return o
end

function App:execute(context)

end

function App:prepare(context)
    local ok, err = pcall(function()
        self:execute(context);
    end);

    if not ok then
        context.reason(tostring(err));
    end

    context.interrupt();
end

function App:main(context)
end

return App;
