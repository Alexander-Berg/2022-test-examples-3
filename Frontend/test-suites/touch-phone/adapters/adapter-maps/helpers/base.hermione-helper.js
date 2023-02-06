/* globals it */
'use strict';

const PO = require('../../../../../page-objects/touch-phone/index').PO;

module.exports = function(expectedUrl) {
    // eslint:global it
    hermione.only.notIn('winphone', 'winphone не поддерживает target: _blank у ссылок');
    it('Проверить общие ссылки и счётчики колдунщика топонимов', function() {
        return this.browser
            .yaCheckSnippet(PO.toponym, {
                title: {
                    url: {
                        href: expectedUrl
                    },
                    baobab: {
                        path: '/$page/$main/$result[@wizard_name="maps"]/title'
                    }
                }
            })
            .yaCheckLink(PO.toponym.map())
            .then(url => this.browser
                .yaCheckURL(url, expectedUrl, 'Сломана ссылка на карте')
            )
            .yaCheckBaobabCounter(PO.toponym.map(), {
                path: '/$page/$main/$result[@wizard_name="maps"]/map'
            });
    });
};
