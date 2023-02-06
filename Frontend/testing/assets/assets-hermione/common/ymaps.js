BEM.decl('map2__pin', {}, { BALLOON_ENABLE_AUTOPAN: false });

BEM.DOM.decl({ name: 'map2', modName: 'controls', modVal: 'yes' }, {
    _onRegionData: function(geoAPIData, userData, callback) {
        function patchedCallback() {
            if (callback) {
                callback.apply(this, arguments);
            }

            this.isYourPinLoaded = true;
        }

        // Этот колбэк вызывается после установки пина юзера и центрирования карты на этом пине
        // См. https://nda.ya.ru/3VoUd4#L76
        return this.__base.call(this, geoAPIData, userData, patchedCallback.bind(this));
    }
});
