BEM.DOM.decl('ajax-updater', {
    _onUpdate: function(_, data) {
        var ajaxUrl = data.ajaxUrl || '';

        if (/^\/search\/result\/(touch|pad)/.test(ajaxUrl) || /^\/search\/(touch|pad)\/pre/.test(ajaxUrl)) {
            throw new Error('Передан неправильный урл в ajax-updater: ' + ajaxUrl);
        }

        return this.__base.apply(this, arguments);
    }
});
