BEM.DOM.decl({block: 'm-orange'}, {
    onSetMod: {
        js: function() {
           this.__base.apply(this);
           this.hasMod('new') && this._setCounter(7);
        }
    },
    _onClick: function(e) {
        var messages = {title: BEM.I18N('m-orange', 'load_error'), button: BEM.I18N('m-orange', 'repeat_request')};

        if(!this._parent) {
            this._parent = this.findBlockOutside(this.params.parentOffset);
        }

        this._showPopup();
        this._setErrorButton(messages, 'append');
    }
});
