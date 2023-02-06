// Очистка страницы от уже существующих примеров.
BEM.DOM.decl('gemini', {
    onSetMod: {
        js: {
            inited: function() {
                var modal = this.findBlockInside('gemini-modal').findBlockInside('button'),
                    modalY = this.findBlockInside('gemini-modal-y').findBlockInside('button');

                modal.bindTo('pointerclick', this._handler.bind(this));
                modalY.bindTo('pointerclick', this._handler.bind(this));
            }
        }
    },

    _bind: function(dom, handler) {
        this
            .findBlockInside(dom, 'button')
            .domElem
            .on('click', handler);
    },

    _handler: function(e) {
        var all = $('.gemini').children();

        setTimeout(function() {
            this.findBlockInside($(e.target).closest('div'), 'user')
                ._domik
                ._popup
                .on('hide', function() {
                    all.show();
                });
        }.bind(this), 25);

        all.hide();
    }
});
