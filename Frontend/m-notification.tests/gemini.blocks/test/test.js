BEM.DOM.decl('test', {
    onSetMod: {
        js: {
            inited: function() {
                this.findBlockInside('m-notification').setMod('activated', 'yes');
            }
        }
    }
});
