var yandexHTML5BannerApi = (function () {

    var _clickUrls = [];

    try {
        var data = ${AUCTION_DC_PARAMS};
        data = data.data_params;

        function getOrderKey(str) {
            var re = /\d+$/;
            var match = str.match(re);
            return match ? parseInt(match[0], 10) : 0;
        }

        for (var c in data) {
            if (c !== "misc" && data[c] && data[c].click_url) {
                var clickUrl = data[c].click_url;
                var clicUrlKeys = [];

                for (var u in clickUrl) {
                    clicUrlKeys.push(u);
                }

                clicUrlKeys.sort(function (a, b) {
                    a = getOrderKey(a);
                    b = getOrderKey(b);
                    return a - b;
                });

                for (var i = 0; i < clicUrlKeys.length; i++) {
                    _clickUrls.push(clickUrl[clicUrlKeys[i]]);
                }

                break;
            }
        }

    } catch (e) {
    }

    function getClickMacro() {
        var clickMacro = '';
        var res = /click_macro=([^&$]+)/.exec(getBaseURI());
        if (res) {
            try {
                clickMacro = decodeURIComponent(res[1]);
            } catch (err) {
            }
        }
        return clickMacro;
    }

    function getBaseURI() {
        var baseURI = document.baseURI;
        if (baseURI === null || typeof baseURI === 'undefined') {
            var baseTag = document.querySelector('base');
            if (baseTag) {
                baseURI = baseTag.href;
            } else {
                baseURI = document.URL;
            }
        }
        return baseURI;
    }

    var clickMacro = getClickMacro();
    return {
        getClickURLNum: function (num) {
            var url = _clickUrls[--num];
            if (clickMacro) {
                return "" + clickMacro + encodeURIComponent(url);
            }
            return url;
        }
    };
})();
