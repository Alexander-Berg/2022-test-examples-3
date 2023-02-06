local parent = require('parent');
local App = parent.BaseApp:new();

local function make_app()
    return App:new {}
end

function App:main(context)
    local request = context.getRequest()
    local bin = context.getBinCb(request:getCardBin())
    if bin ~= nil then
        context.logInfo("default script for order", request:getOrderId(), ", bin", bin:getBin())
        context.addToQueue("default script for order", request:getOrderId(), ", bin", bin:getBin())
    else
        context.logError("default script for order", request:getOrderId())
        context.addToQueue("default script for order", request:getOrderId())
    end
end
