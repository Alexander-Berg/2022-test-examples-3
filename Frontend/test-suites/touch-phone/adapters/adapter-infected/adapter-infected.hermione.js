'use strict';

const PO = require('../../../../page-objects/touch-phone/index').PO;

specs('Завирусованный сниппет', function() {
    beforeEach(function() {
        return this.browser
            .yaOpenSerp('text=infected.cepera.ru site:infected.cepera.ru')
            .yaWaitForVisible(PO.infected(), 'Не появился завирусованный сниппет');
    });

    it('Проверка ссылок гринурла и тайтла сниппета', function() {
        return this.browser
            .yaCheckSnippet(PO.infected, {
                title: {
                    baobab: {
                        path: '/$page/$main/$result[@infected=true]/title'
                    }
                },
                greenurl: [{
                    baobab: {
                        path: '/$page/$main/$result[@infected=true]/path/urlnav'
                    }
                }]
            });
    });

    it('Проверка ссылки и счётчика текста с предупреждением', function() {
        return this.browser
            .yaCheckLink(PO.infected.warning())
            .yaCheckBaobabCounter(PO.infected.warning(), {
                path: '/$page/$main/$result[@infected=true]/warning'
            });
    });
});
