module.exports = function(bh) {
    bh.match('test-i-global-api', function(ctx, json) {
        ctx.tag(false);

        var glob = bh.lib.global;
        if(json.pub) {
            glob.makePublic(json.pub);
        }
        if(json.priv) {
            glob.makePublic(json.priv, false);
        }
        if(json.bulk) {
            glob.makePublic(json.bulk);
        }
        if(json.tld) {
            glob.setTld(json.tld);
        }
    });
};
