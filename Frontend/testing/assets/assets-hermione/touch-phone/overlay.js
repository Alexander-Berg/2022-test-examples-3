BEM.DOM.decl('overlay', {
    onSetMod: {
        js: function() {
            this.__base.apply(this, arguments);

            BEM.channel('overlay').on('onAfterOverlayOpen', function(event, data) {
                window.hermione.meta.overlayReqId = data.reqid;
            }, this);

            BEM.channel('overlay').on('close', function() {
                window.hermione.meta.overlayReqId = null;
            }, this);
        }
    },

    _writeItemsToStorage: function(items) {
        items = $.map(items, function(item) {
            item = $(item);

            var iframe = item.find('.overlay-iframe__frame'),
                tpidSrc = BEM.blocks.uri
                    .parse(iframe.attr('src'))
                    .addParam('tpid', window.hermione.meta.tpid)
                    .build();

            if (iframe.attr('src') !== 'about:blank') {
                iframe.attr('src', tpidSrc);
            }

            return item.get(0).outerHTML;
        });

        this.__base.call(this, items);
    }
}, {
    WAITING_FULL_SCREEN_TIMEOUT: 0
});
