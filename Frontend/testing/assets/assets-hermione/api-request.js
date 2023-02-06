modules.define('api-request', ['i-bem-dom'], function(provide, bemDom, ApiRequest) {
    if (!ApiRequest) {
        provide(ApiRequest);

        return;
    }

    var ApiRequestWithTpid = bemDom.declBlock(ApiRequest, {
        _buildRequestUrl: function() {
            var requestUrl = this.__base.apply(this, arguments);

            var tpid = null;
            window.location.search.slice(1).split('&').forEach(function(part) {
                if (/^tpid=(.*?)$/i.test(part)) {
                    tpid = RegExp.$1;
                }
            });

            return requestUrl + '&tpid=' + tpid;
        },
    });

    provide(ApiRequestWithTpid);
});
