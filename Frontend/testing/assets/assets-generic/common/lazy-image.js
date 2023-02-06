BEM.DOM.decl({ name: 'lazy-image', modName: 'type', modVal: 'bounds' }, {
    onSetMod: {
        js: function() {
            this.__base.apply(this, arguments);

            this.loadImages(null, 0);
        }
    }
}, {
    isWithinBounds: function() {
        return true;
    },

    isWithinWindow: function() {
        return true;
    }
});
