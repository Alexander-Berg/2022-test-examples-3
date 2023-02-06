'use strict';

const PO = require('./OfferInfo.page-object');

async function checkVisuallyHiddenBlock(a11y, text, message) {
    const a11yText = await this.browser.execute(function(selector) {
        return document.querySelector(selector).textContent;
    }, a11y);

    assert.equal(text, a11yText, message);
}

specs({
    feature: 'Офферы',
    type: 'Офферное описание',
}, function() {
    it('Внешний вид', async function() {
        await this.browser.yaOpenSerp({
            text: 'огурец молодец',
            foreverdata: '4200449830',
            data_filter: 'offer_info',
            srcskip: 'YABS_DISTR',
        }, PO.serpItem());

        await this.browser.yaScroll(PO.serpItem());
        await this.browser.assertView('offer-info', PO.serpItem());
    });

    it('Собственная ссылка у тумбы', async function() {
        const { browser } = this;

        await browser.yaOpenSerp({
            text: 'огурец молодец',
            foreverdata: '4200449830',
            data_filter: 'offer_thumb',
            srcskip: 'YABS_DISTR',
        }, PO.serpItem());

        const thumbLink = await browser.getAttribute(PO.serpItem.thumb(), 'href');
        const titleLink = await browser.getAttribute(PO.serpItem.url(), 'href');

        assert.notEqual(thumbLink, titleLink);
    });

    hermione.only.in('chrome-phone', 'Не браузерозависимо');
    describe('Поддержка a11y в офферах', () => {
        it('Цены и тумба', async function() {
            const { browser } = this;

            await browser.yaOpenSerp({
                foreverdata: '1278455664',
            }, PO.serpList());

            // цена
            assert.equal(await browser.getAttribute(PO.serpItem.priceRange(), 'role'), 'text', 'role у цены указан неверно');
            assert.equal(await browser.getAttribute(PO.serpItem.priceRange(), 'tabindex'), '-1', 'tabindex у цены указан неверно');
            await checkVisuallyHiddenBlock.call(this, PO.serpItem.priceRange.a11yHidden(), 'Цена:', 'некорректный скрытый a11y текст у цены');
            assert.equal(await browser.getAttribute(PO.serpItem.priceRange.Rub(), 'aria-label'), 'руб.', 'некорректный aria-label у валюты цены');

            // старая цена
            assert.equal(await browser.getAttribute(PO.serpItem.priceOutdated.currentValue(), 'role'), 'text', 'role у старой цены указан неверно');
            assert.equal(await browser.getAttribute(PO.serpItem.priceOutdated.currentValue(), 'tabindex'), '-1', 'tabindex у старой цены указан неверно');
            await checkVisuallyHiddenBlock.call(this, PO.serpItem.priceOutdated.currentValue.a11yHidden(), 'Старая цена:', 'некорректный скрытый a11y текст у старой цены');
            assert.equal(await browser.getAttribute(PO.serpItem.priceOutdated.currentValue.Rub(), 'aria-label'), 'руб.', 'некорректный aria-label у валюты старой цены');

            // тумба
            assert.equal(await browser.getAttribute(PO.serpItem.thumb(), 'aria-label'), 'Огурцы бакинские темные | Moscowfresh');
            assert.equal(await browser.getAttribute(PO.serpItem.thumb(), 'role'), 'img', 'role у изображения-ссылки указан неверно');
        });
    });
});
