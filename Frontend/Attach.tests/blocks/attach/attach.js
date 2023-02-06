BEM.DOM.decl('attach', {
    onSetMod: {
        js: {
            inited: function() {
                this.__base.apply(this, arguments);

                if(this.params.ext) {
                    this._setFilePath('dummy.' + this.params.ext);
                }
            }
        }
    }
}, {});
