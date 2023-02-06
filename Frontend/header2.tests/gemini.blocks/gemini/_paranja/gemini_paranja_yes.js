BEM.DOM.decl({block: 'gemini', modName: 'paranja', modVal: 'yes'}, {
    onSetMod: {
        js: {
            inited: function() {
                this.__base();
                this.findBlockInside('header2').bindParanjaToPanel('settings');
            }
        }
    }
});
