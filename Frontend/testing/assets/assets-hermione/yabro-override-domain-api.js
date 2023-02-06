/**
 * Имитирует api Яндекс Браузера, которое меняет домен в адресной строке.
 */
(function() {
    if (window.location.search.indexOf('yabro-override-domain-api=1') === -1) {
        return;
    }

    window.yandex || (window.yandex = {});
    window.yandex.publicFeature || (window.yandex.publicFeature = {});

    window.yandex.publicFeature.overrideTabHost = function(host) {
        window.overridedDomain = host;
    };
})();
