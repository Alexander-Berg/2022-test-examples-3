BEM.DOM.decl('overlay-iframe', {
    _changeSrc: function(intent) {
        if (this._src !== intent.iframeUrl) {
            var tpidSrc = BEM.blocks.uri
                .parse(intent.iframeUrl)
                .addParam('tpid', window.hermione.meta.tpid)
                .build();

            this._iframe.attr('src', tpidSrc);
            this._src = tpidSrc;
        }
    }
});
