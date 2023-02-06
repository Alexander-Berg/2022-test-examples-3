(function(document) {
    'use strict';

    function isDocumentReady() {
        return document.readyState === 'complete';
    }

    function isBemInited() {
        return window.BEM._serpInited;
    }

    function hideAds() {
        var e = document.querySelectorAll('[' + window.rc.adAttrName + ']');
        for (var i = 0; i < e.length; ++i) {
            e[i].style.display = 'none';
        }
    }

    var timer = setInterval(function() {
        if (isDocumentReady() && isBemInited()) {
            clearInterval(timer);
            setTimeout(hideAds, 1500);
        }
    }, 300);
})(document);
