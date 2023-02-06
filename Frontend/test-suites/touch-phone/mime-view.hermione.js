'use strict';

const PO = require('../../page-objects/common/index').PO;

specs('Спецсниппет с документом', function() {
    beforeEach(function() {
        return this.browser
            .yaOpenSerp('text=pdf site:ikea.com')
            .yaWaitForVisible(PO.serpList.mimeViewSnippet(), 'Спецсниппет с документом появился на странице');
    });

    it('Проверка ссылок и счётчиков', function() {
        return this.browser
            .yaCheckSnippet(PO.serpList.mimeViewSnippet, {
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
                    selector: PO.serpList.mimeViewSnippet.metaLink(),
                    baobab: {
                        path: '/$page/$main/$result/serp-meta/line/view'
                    }
                }]
            });
    });
});
