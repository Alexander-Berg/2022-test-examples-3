'use strict';

const PO = require('../../../page-objects/touch-phone/index').PO;

specs('Рейтинг в сниппетах', function() {
    beforeEach(function() {
        return this.browser
            .yaOpenSerp('text=мультик зверополис')
            .yaWaitForVisible(PO.snipRating(), 'Сниппет с рейтингом должен быть в выдаче');
    });

    it('Проверка ссылок и счётчиков', function() {
        return this.browser
            .yaCheckSnippet(PO.snipRating, {
                title: {
                    baobab: {
                        path: '/$page/$main/$result/title'
                    }
                },
                greenurl: [{
                    baobab: {
                        path: '/$page/$main/$result/path/urlnav'
                    }
                }]
            });
    });
});
