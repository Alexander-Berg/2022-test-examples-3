'use strict';

const PO = require('./RandomImageGame.page-object')('touch-phone');

hermione.only.notIn(['iphone', 'searchapp-phone'], 'В iOS < 14 версии нет кнопки');
specs('Колдунщик рандомных изображений с поделяшкой', function() {
    beforeEach(async function() {
        await this.browser.yaOpenSerp(
            {
                text: 'foreverdata',
                data_filter: 'random-image-game',
                exp_flags: 'freshness_random_image_share=1',
                foreverdata: '3849158746',
            },
            PO.randomImageGame(),
        );
    });

    it('Внешний вид', async function() {
        await this.browser.assertView('random-image-game-share', PO.randomImageGame());
    });

    it('Счётчик', async function() {
        await this.browser.yaCheckBaobabCounter(PO.randomImageGame.share(), {
            path: '/$page/$main/$result/randon-image-game/share-button',
        });
    });
});
