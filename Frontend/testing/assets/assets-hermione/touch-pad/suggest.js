(function() {
    // Если в URL есть параметр save_suggest_reqid, то сохраняем suggest_reqid в параметре suggest_reqid_saved
    // Нужно для тестов, проверяющих отправку suggest_reqid
    BEM.DOM.decl('search2', {
        serialize: function(extraParams) {
            var result = this.__base.apply(this, arguments);

            if (location.href.indexOf('&save_suggest_reqid') > 0) {
                var parsedUrl = BEM.blocks.uri.parse('https://example.com/?' + result),
                    suggestReqid = (parsedUrl.getParam('suggest_reqid') || [])[0];
                result += '&save_suggest_reqid=1&suggest_reqid_saved=' + suggestReqid;
            }

            if (location.href.indexOf('&save_suggest_state') > 0) {
                result += '&save_suggest_state=1';
            }

            return result;
        }
    });
})();
