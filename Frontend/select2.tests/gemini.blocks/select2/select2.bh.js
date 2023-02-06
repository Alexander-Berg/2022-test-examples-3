module.exports = function(bh) {
    bh.match('select2', function(ctx, json) {
        ctx.tParam('selectCls', json.cls);
    });

    bh.match('popup2', function(ctx) {
        var cls = ctx.tParam('selectCls');
        if(cls) {
            ctx.cls(cls + '-popup');
        }
    });
};
