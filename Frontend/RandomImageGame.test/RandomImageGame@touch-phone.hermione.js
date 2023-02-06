'use strict';

const PO = require('./RandomImageGame.page-object')('touch-phone');

specs('Колдунщик рандомных изображений', function() {
    beforeEach(async function() {
        await this.browser.yaOpenSerp(
            {
                text: 'foreverdata',
                data_filter: 'random-image-game',
                foreverdata: '3871153235',
            },
            PO.randomImageGame(),
        );
    });

    it('Внешний вид', async function() {
        await this.browser.assertView('random-image-game', PO.randomImageGame());
    });

    it('Жалобщик', async function() {
        await this.browser.yaCheckBaobabCounter(PO.randomImageGame.secondLink(), {
            path: '/$page/$main/$result/randon-image-game/feedback',
        });

        await this.browser.yaWaitForVisible(PO.feedbackDialog());

        await this.browser.assertView('feedback-dialog', PO.feedbackDialog(), {
            allowViewportOverflow: true,
            compositeImage: false,
            captureElementFromTop: false,
        });
    });

    it('Счетчики и ссылки', async function() {
        await this.browser.yaCheckBaobabCounter(() => {}, {
            path: '/$page/$main/$result/randon-image-game',
            event: 'tech',
            type: 'show-random-image-game',
            data: { imageId: 'image_id_0' },
        });

        await this.browser.yaCheckLink2({
            selector: PO.randomImageGame.firstLink(),
            target: '_blank',
            baobab: {
                path: '/$page/$main/$result/randon-image-game/source-link',
            },
        });
    });
});
