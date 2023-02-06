BEM.DOM.decl('user-location-updater', {
    onSetMod: {
        js: {
            inited: function() {
                this.deferred = true; // откладываю updateUserLocation, пока не мокну Permission API
                this.__base.apply(this, arguments);
            }
        }
    }
});
