BEM.DOM.decl('avia-dynamic-best-offers', {
    onSetMod: {
        js: {
            inited: function() {
                this.__base.apply(this, arguments);

                var offersEl = this.findElem('offers');

                offersEl.on('click', function(e) {
                    e.preventDefault();
                });
            }
        }
    },

    // Открываем пустую страницу, чтобы не ломать тесты
    _openWindow: function() {
        this.__base('about:blank');
    }
});
