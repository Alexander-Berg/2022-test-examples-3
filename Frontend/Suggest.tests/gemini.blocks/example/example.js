BEM.DOM.decl('example', {
    onSetMod: {
        js: {
            inited: function() {
                var input = this.findBlockInside('input');

                this._suggest = BEM.create('suggest', {
                    owner: this,
                    input: input,

                    url: '//suggest.yandex.ru/suggest-ya.cgi',

                    // Мок-провайдер данных, см. блок `mock-data`.
                    name: 'mock-data',

                    suggest: {
                        srv: 'morda_ru',
                        wiz: 'TrWth',
                        lr: 213
                    }
                });
            }
        }
    }
});
