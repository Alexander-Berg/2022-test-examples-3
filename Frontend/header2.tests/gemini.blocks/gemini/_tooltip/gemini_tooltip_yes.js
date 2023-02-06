BEM.DOM.decl({block: 'gemini', modName: 'tooltip', modVal: 'yes'}, {
    onSetMod: {
        js: {
            inited: function() {
                this.__base();

                this._header = this.findBlockInside('header2');

                this._tooltip = this._header.findBlockInside('tooltip')
                    .setOwner(this._header.elem('action', 'type', 'settings'))
                    .on('hide', this._onTooltipHide, this);

                this._header.on('action-change', this._onSettingsActionSwitch, this);
            }
        }
    },

    _onTooltipHide: function(e, data) {
        this._header.delMod(this._header.elem('action', 'type', 'settings'), 'checked');
    },

    _onSettingsActionSwitch: function(e, data) {
        if(data.type === 'settings') {
            this._tooltip.toggleMod('shown', 'yes', '', data.checked);
        }
    }
});
