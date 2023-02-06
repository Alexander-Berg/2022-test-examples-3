local module = {};

local CLIENT_NAME = "FuryClient";

function module.execute(context, service, callback)
    local ok, error = pcall(function()
        context.client:post(CLIENT_NAME,
            context.client:query("/v2", {}),
            context.client:json {
                jsonrpc = "2.0",
                method = "process",
                id = context.nsrc.txn_extid,
                params = {
                    key = context.nsrc.txn_extid,
                    service = service,
                    type = "transaction",
                    puid = context.nsrc.uid,
                    body = context.src,
                }
            },
            callback);
    end)
    if not ok then
        context.comment(error);
    end
end

return module;
