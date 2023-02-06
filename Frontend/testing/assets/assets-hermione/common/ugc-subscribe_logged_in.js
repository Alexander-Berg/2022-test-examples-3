BEM.DOM.decl({ block: 'ugc-subscribe', modName: 'logged', modVal: 'in' }, {
    _resolveEnabled: function() {
        // описание тут: construct/blocks-common/supply/__ugc-subscribe/supply__ugc-subscribe.priv.js

        if (this.params.initialData) {
            this._data = this.params.initialData;
            delete this.params.initialData;
        }

        var newSubscribeVal = (this._data && this._data.OntoId === this.params.data.OntoId) ? 'yes' : 'no';
        this.setMod('subscribed', newSubscribeVal);
    }
});
