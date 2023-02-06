block('test-i-global-api')(
    tag()(false),

    def()(function() {
        var glob = this['i-global'],
            ctx = this.ctx;
        if(ctx.pub) {
            glob.makePublic(ctx.pub);
        }
        if(ctx.priv) {
            glob.makePublic(ctx.priv, false);
        }
        if(ctx.bulk) {
            glob.makePublic(ctx.bulk);
        }
        if(ctx.tld) {
            glob.setTld(ctx.tld);
        }
        // В `bem-xjst@next` нужно возвращать пустую строку в моде `def`,
        // иначе в результат попадёт `undefined`.
        // https://github.com/bem/bem-xjst/issues/74
        return '';
    })
);
