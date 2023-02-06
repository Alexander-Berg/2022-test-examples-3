'use strict';

const PO = require('../../../page-objects/touch-phone/index').PO;

specs('Расширенный сниппет / Включение расширенного сниппета', function() {
    beforeEach(function() {
        return this.browser
            .yaOpenSerp('text=nba')
            .yaWaitForVisible(PO.extendedSnippet(), 'Не появился расширенный сниппет');
    });

    it('Проверка ссылок и счётчиков', function() {
        return this.browser
            .yaCheckSnippet(PO.extendedSnippet, {
                title: {
                    baobab: {
                        path: '/$page/$main/$result/title'
                    }
                },
                greenurl: [{
                    baobab: {
                        path: '/$page/$main/$result/path/urlnav'
                    }
                }],
                sitelinks: [{
                    selector: PO.extendedSnippet.firstSitelink(),
                    baobab: { path: '/$page/$main/$result/sitelinks/item' }
                }]
            });
    });
});
