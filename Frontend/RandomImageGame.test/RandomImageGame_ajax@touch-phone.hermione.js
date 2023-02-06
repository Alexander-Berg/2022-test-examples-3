'use strict';

const PO = require('./RandomImageGame.page-object')('touch-phone');

specs('Колдунщик рандомных изображений', function() {
    beforeEach(async function() {
        await this.browser.yaOpenSerp(
            {
                text: 'кто я из жаб',
                srcrwr: [
                    'UPPER:artemon.sas.yp-c.yandex.net:13372',
                    'BEGEMOT_WORKER_P:artemon.sas.yp-c.yandex.net:8867:2s',
                    'SRC_SETUP:artemon.sas.yp-c.yandex.net:17190',
                ],
                data_filter: 'random-image-game',
            },
            PO.randomImageGame(),
        );
    });

    hermione.only.notIn('searchapp-phone');
    it('Аякс', async function() {
        await this.browser.assertView('random-image-game', PO.randomImageGame());

        await this.browser.yaCheckBaobabCounter(() => {}, {
            path: '/$page/$main/$result/randon-image-game',
            event: 'tech',
            type: 'show-random-image-game',
        });

        await this.browser.yaCheckBaobabCounter(PO.randomImageGame.refresh(), {
            path: '/$page/$main/$result/randon-image-game/try-again-button',
        });

        await this.browser.yaWaitForHidden(PO.randomImageGame.spin());

        await this.browser.assertView('random-image-game-after-refresh', PO.randomImageGame());

        await this.browser.yaCheckBaobabCounter(() => {}, {
            path: '/$page/$main/$result/randon-image-game',
            event: 'tech',
            type: 'show-random-image-game',
        });
    });
});
