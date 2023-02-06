block('select2')(
    def()(function() {
        return applyNext({_selectCls: this.ctx.cls});
    })
);

block('popup2').match(function() { return this._selectCls !== undefined; })(
    def()(function() {
        this.ctx.cls = this._selectCls + '-popup';
        delete this._selectCls;
        return applyNext();
    })
);
