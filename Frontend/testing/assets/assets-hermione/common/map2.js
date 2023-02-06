BEM.DOM.decl({ block: 'map2', modName: 'type', modVal: 'dynamic' }, {
    isVisibleCarparksLayer: function() {
        return Boolean(this._carparkslayer && this._carparkslayer.getMap());
    },

    isVisibleTrafficLayer: function() {
        return Boolean(this._trafficLayer && this._trafficLayer.getMap());
    }
});
