(function(window) {
    var __stubInIframe = window.Ya.isInFrame;
    window.__stubPostMessage = window.Ya.postMessage;
    window.__postMessageList = [];

    window.Ya.isInFrame = function() {
        if (location.search.indexOf('in-iframe') !== -1) {
            return true;
        }

        return __stubInIframe();
    };

    window.addEventListener('message', receiveMessage, false);

    function receiveMessage(event) {
        window.__postMessageList.push(event.data);
    }
})(window);
