BEM.decl({ block: 'deeplink' }, {
    tryLoadAndroidApp: function() {
        window.open(this._url, '_blank');
    }
});
