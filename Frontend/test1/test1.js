// DEPRECATED Удалить в 6.0 код про совместимость https://st.yandex-team.ru/ISL-2936
BEM.DOM.decl('test1', {
    onSetMod: {
        js: {
            inited: function() {
                this._popup = this.findBlockInside('popup');
                this._button = this.findBlockInside('button2');
            }
        }
    },

    _onButtonClick: function() {
        this._popup.toggle(this._button);
    }
}, {
    live: function() {
        this.liveInitOnBlockInsideEvent('click', 'button2', function(e) {
            this._onButtonClick(e);
        });
    }
});
