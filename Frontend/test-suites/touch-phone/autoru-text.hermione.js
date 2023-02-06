'use strict';

const PO = require('../../page-objects/touch-phone/index').PO;

specs('Колдунщик авто общий', function() {
    hermione.only.notIn('winphone', 'winphone не поддерживает target: _blank у ссылок');
    it('Проверка ссылок', function() {
        return this.browser
            .yaOpenSerp('text=автомобили&srcskip=ATOM_PROXY')
            .yaWaitForVisible(PO.autoText(), 'Колдунщик авто не появился')
            .yaCheckSnippet(PO.autoText, {
                title: {
                    baobab: {
                        path: '/$page/$main/$result[@wizard_name="autoru/thumbs-text"]/title'
                    }
                },
                greenurl: [{
                    baobab: {
                        path: '/$page/$main/$result[@wizard_name="autoru/thumbs-text"]/path/urlnav'
                    }
                }]
            });
    });
});
