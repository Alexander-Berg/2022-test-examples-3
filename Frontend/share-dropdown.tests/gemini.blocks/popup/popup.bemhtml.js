block('popup')
    .match(function() { return (this.ctx.js || {}).shareDropdown; })
    .attrs()(function() {
        var a = applyNext() || {};
        a['data-share-dropdown'] = this.ctx.js.shareDropdown;
        return a;
    });
