BEM.DOM.decl('test', {
    onSetMod: {
        js: {
            inited: function() {
                this._modal = this.findBlockInside('popup');
                this.bindTo('opener', 'pointerclick', this._onOpen);
            }
        }
    },

    _onOpen: function() {
        this._modal.toggle();
    }
});
