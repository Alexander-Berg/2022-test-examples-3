(function() {
    window.hermione.xhrSpies = [];

    const proto = XMLHttpRequest.prototype;
    const originalOpen = proto.open;
    const originalSend = proto.send;

    proto.open = function(method, url) {
        window.hermione.xhrSpies.push(this);

        this.spy = {
            method,
            url,
        };

        return originalOpen.apply(this, arguments);
    };

    proto.send = function(body) {
        this.spy.body = body;

        return originalSend.apply(this, arguments);
    };
})();
