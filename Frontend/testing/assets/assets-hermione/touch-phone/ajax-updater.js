BEM.DOM.decl('ajax-updater', {
    _onUpdate: function(_, data) {
        var ajaxUrl = data.ajaxUrl || '';

        // eslint-disable-next-line no-unsafe-regex/no-unsafe-regex
        var searchResult = ajaxUrl.match(/^\/search\/result(\/([^/?]+)?)/);
        if (searchResult && searchResult[2] !== 'touch') {
            throw new Error('Передан неправильный урл в ajax-updater: ' + ajaxUrl);
        }

        // eslint-disable-next-line no-unsafe-regex/no-unsafe-regex
        var searchPre = ajaxUrl.match(/^\/search\/(([^/]+)\/)?pre/);
        if (searchPre && searchPre[2] !== 'touch') {
            throw new Error('Передан неправильный урл в ajax-updater: ' + ajaxUrl);
        }

        return this.__base.apply(this, arguments);
    }
});
