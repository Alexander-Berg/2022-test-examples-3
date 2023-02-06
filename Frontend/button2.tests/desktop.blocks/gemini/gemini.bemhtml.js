block('gemini')(
    attrs()(function() {
        return {id: this.ctx.id};
    }),

    elem('item')
        .match(function() { return this.ctx.data; })
        .cls()(function() {
            return Object.keys(this.ctx.data)
                .map(function(key) {
                    return key + '_' + this.ctx.data[key];
                }, this)
                .join(' ');
        })
);
