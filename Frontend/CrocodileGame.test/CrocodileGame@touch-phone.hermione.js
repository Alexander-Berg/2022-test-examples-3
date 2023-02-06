'use strict';

const { hideDrawerHandle } = require('../../../../hermione/client-scripts/hide-drawer-handle_touch');
const PO = require('./CrocodileGame.page-object');

specs('Колдунщик крокодила', () => {
    // eslint-disable-next-line camelcase
    const data_filter = false;
    const srcskip = 'YABS_DISTR';
    const text = 'foreverdata';
    const foreverdata = '496380820';
    const baobabPath = '/$page/$main/$result[@wizard_name="crocodile_game"]';

    it('Внешний вид', async function() {
        const { browser } = this;

        await browser.yaOpenSerp({ text, foreverdata, data_filter, srcskip }, PO.crocodileGame());
        await browser.yaCheckBaobabServerCounter({ path: baobabPath });
        await browser.assertView('initial', PO.crocodileGame());
    });

    hermione.only.notIn(['iphone'], 'orientation is not supported');
    it('Внешний вид в горизонтальной ориентации', async function() {
        const { browser } = this;

        await browser.yaOpenSerp({ text, foreverdata, data_filter, srcskip }, PO.crocodileGame());
        await browser.setOrientation('landscape');
        await browser.assertView('horizontal', PO.crocodileGame());
    });

    it('Смена слова', async function() {
        const { browser } = this;

        await browser.yaOpenSerp({ text, foreverdata, data_filter, srcskip }, PO.crocodileGame());
        await browser.click(PO.crocodileGame.submitButton());
        await browser.waitForVisible(PO.crocodileGame.submitButton());
        const firstWord = await browser.getText(PO.crocodileGame.value());
        await browser.click(PO.crocodileGame.submitButton());
        const secondWord = await browser.getText(PO.crocodileGame.value());
        assert(firstWord !== secondWord, 'Слово не сменилось');
    });

    hermione.only.notIn(['searchapp-phone'], 'Не работает подскролл контента попапа');
    it('Шторка обратной связи', async function() {
        const { browser } = this;

        await browser.yaOpenSerp({
            text: 'крокодил игра',
        }, PO.crocodileGame());

        await browser.click(PO.crocodileGame.reportButton());

        await browser.waitForVisible(PO.feedbackDialog(), 1000);

        await browser.execute(hideDrawerHandle);

        await browser.assertView('feedback-drawer', PO.feedbackDialog(), {
            selectorToScroll: '.Drawer-Content',
            hideElements: ['body > :not(.Popup2_visible)'],
        });
    });
});
