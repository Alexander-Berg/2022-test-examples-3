'use strict';

const {
    MarketOffersWizard,
    ExtralinksPopup,
} = require('../../../Market.test/Market.page-object');

specs({
    feature: 'Маркет. Офферный колдунщик в центре',
    type: 'Дебрендированный офферный колдунщик',
}, () => {
    it('Внешний вид дебрендиронного колдунщика', async function() {
        await this.browser.yaOpenSerp({
            foreverdata: 3356472073,
        }, MarketOffersWizard());

        await this.browser.assertView('debranding_offers_wizard', MarketOffersWizard());
    });

    it('Внешний вид дебрендиронного без рейтинга', async function() {
        await this.browser.yaOpenSerp({
            foreverdata: 3356472073,
        }, MarketOffersWizard());

        await this.browser.assertView('debranding_offers_wizard_without_rating', MarketOffersWizard());
    });

    it('Внешний вид врезки с карточками sku', async function() {
        await this.browser.yaOpenSerp({
            foreverdata: 1045567449,
        }, MarketOffersWizard());

        await this.browser.assertView('with_sku_card', MarketOffersWizard());
    });

    it('Внешний вид дебрендиронного колдунщика с расширенным заголовком', async function() {
        await this.browser.yaOpenSerp({
            foreverdata: 2822530097,
            exp_flags: [
                'PRODUCTS_disable_chevron_desktop=10',
                'PRODUCTS_disable_chevron_touch=10',
            ],
        }, MarketOffersWizard());

        await this.browser.assertView('debranding_offers_wizard', MarketOffersWizard());
    });

    it('Наличие marketincut в счетчике колдунщика', async function() {
        await this.browser.yaOpenSerp({
            foreverdata: 3356472073,
            exp_flags: [
                'MARKET_add_marketincut_param=1',
            ],
        }, MarketOffersWizard());

        await this.browser.yaCheckBaobabServerCounter({
            path: '/$page/$main/$result/MarketOffersWizard[@marketincut=true]',
        });
    });

    it('Ссылка на оффер ТВ', async function() {
        await this.browser.yaOpenSerp({
            foreverdata: 3356472073,
        }, MarketOffersWizard());

        const card = MarketOffersWizard.productCard;
        const expectedOfferFirstLink = '/products/offer/zyiQ4G5H4oqeHRtkIkzWlQ?text=';
        const titleLinkHref = await this.browser.getAttribute(card.title.link(), 'href');
        assert.include(titleLinkHref, expectedOfferFirstLink, 'некорректная ссылка в названии товара');
        const greenUrlLinkHref = await this.browser.getAttribute(card.greenUrl.link(), 'href');
        assert.include(greenUrlLinkHref, expectedOfferFirstLink, 'некорректная ссылка в названии магазина');
        const priceLinkHref = await this.browser.getAttribute(card.price.link(), 'href');
        assert.include(priceLinkHref, expectedOfferFirstLink, 'некорректная ссылка в цене товара');
        const overlayLinkHref = await this.browser.getAttribute(card.linkOverlay(), 'href');
        assert.include(overlayLinkHref, expectedOfferFirstLink, 'некорректная ссылка в накладке над описанием товара');
        const thumbLinkHref = await this.browser.getAttribute(card.thumb(), 'href');
        assert.include(thumbLinkHref, expectedOfferFirstLink, 'некорректная ссылка в картинке товара');
    });

    it('Punycode отображается корректно', async function() {
        await this.browser.yaOpenSerp({
            foreverdata: 333914333,
        }, MarketOffersWizard());

        await this.browser.assertView('offers_wizard_punycode_correct', MarketOffersWizard());
    });

    it('Фильтры-кнопки ведущие на ТВ', async function() {
        await this.browser.yaOpenSerp({
            foreverdata: 3356472073,
            exp_flags: [
                'PRODUCTS_offers_wizard_fake_filters=1',
            ],
        }, MarketOffersWizard());

        await this.browser.assertView('goods_offers_wizard_with_filters', MarketOffersWizard());
    });

    it('Внешний вид sku с лучшей ценой', async function() {
        await this.browser.yaOpenSerp({
            foreverdata: 2856234311,
            exp_flags: [
                'PRODUCTS_sku_best_price_label=Лучшая цена',
            ],
        }, MarketOffersWizard());

        await this.browser.assertView('best-price-3-line', MarketOffersWizard.productCard());
        await this.browser.yaScroll(MarketOffersWizard.productCard2());
        await this.browser.assertView('best-price-2-line', MarketOffersWizard.productCard2());
    });

    describe('Экстралинки', async function() {
        beforeEach(async function() {
            await this.browser.yaOpenSerp({
                foreverdata: 3356472073,
            }, MarketOffersWizard());

            await this.browser.click(MarketOffersWizard.extralinks());
            await this.browser.yaWaitForVisible(ExtralinksPopup());
            const links = await this.browser.$$(ExtralinksPopup.link());
            assert.lengthOf(links, 3, 'в экстралинках должно быть три ссылки');
        });

        it('Пожаловаться', async function() {
            const feedback = await this.browser.$(ExtralinksPopup.firstLink());
            const href = await feedback.getAttribute('href');
            const url = new URL(href);
            const { searchParams } = url;
            assert.isTrue(searchParams.has('reqid'), 'в cgi-параметрах ссылки нет параметра reqid');
            assert.isTrue(searchParams.has('query'), 'в cgi-параметрах ссылки нет параметра query');
            assert.isTrue(searchParams.has('service'), 'в cgi-параметрах ссылки нет параметра service');
            assert.isTrue(searchParams.has('answer_non_profile_email_10233409'), 'в cgi-параметрах ссылки нет параметра с почтой');
            assert.isTrue(searchParams.has('answer_short_text_10233408'), 'в cgi-параметрах ссылки нет параметра с именем');
            assert.isTrue(searchParams.has('offerIds'), 'в cgi-параметрах ссылки нет параметра offerIds');
            assert.strictEqual(
                url.origin + url.pathname,
                'https://forms.yandex.ru/surveys/10033886.3595e12b1f26aae821d591a13d388310aa7ed8e1/',
                'некорректная ссылка на форму «Пожаловаться»',
            );
            assert.strictEqual(
                await feedback.getAttribute('target'),
                '_blank',
                'ссылка «Пожаловаться» должна открываться в новой вкладке',
            );
            assert.strictEqual(
                await feedback.getText(),
                'Пожаловаться',
                'текст ссылки должен быть «Пожаловаться»',
            );
        });

        it('Условия подключения', async function() {
            const connect = await this.browser.$(ExtralinksPopup.secondLink());
            const href = await connect.getAttribute('href');
            assert.strictEqual(
                href,
                'https://yandex.ru/support/products/partners.html',
                'некорректная ссылка на документацию об условиях подключения',
            );
            assert.strictEqual(
                await connect.getAttribute('target'),
                '_blank',
                'ссылка «Условия подключения» должна открываться в новой вкладке',
            );
            assert.strictEqual(
                await connect.getText(),
                'Условия подключения',
                'текст ссылки должен быть «Условия подключения»',
            );
        });

        it('Что такое лучшие цены', async function() {
            const connect = await this.browser.$(ExtralinksPopup.thirdLink());
            const href = await connect.getAttribute('href');
            assert.strictEqual(
                href,
                'https://yandex.ru/support/products/best-price.html',
                'некорректная ссылка на документацию о лучших ценах',
            );
            assert.strictEqual(
                await connect.getAttribute('target'),
                '_blank',
                'ссылка «Что такое лучшие цены» должна открываться в новой вкладке',
            );
            assert.strictEqual(
                await connect.getText(),
                'Что такое лучшие цены?',
                'текст ссылки должен быть «Что такое лучшие цены»',
            );
        });
    });

    // Ссылки не являются браузерозависимыми.
    hermione.only.in(['chrome-desktop', 'chrome-phone']);
    describe('Проверка ссылки', () => {
        it('В заголовке', async function() {
            const bro = this.browser;

            await bro.yaOpenSerp({
                foreverdata: 1257384751,
            }, MarketOffersWizard());

            const href = await bro.getAttribute(MarketOffersWizard.debrandingTitle(), 'href');
            assert.include(href, '/products/search', 'в ссылке некорректный путь');

            const { searchParams } = new URL(href);
            assert.isTrue(searchParams.has('rs'), 'в ссылке нет параметра rs');
            assert.isTrue(searchParams.has('parent-reqid'), 'в ссылке нет параметра parent-reqid');
            assert.strictEqual(searchParams.get('utm_source_service'), 'web', 'в ссылке нет параметра utm_source_service или у него неверное значение');
        });

        it('В товаре', async function() {
            const bro = this.browser;

            await bro.yaOpenSerp({
                foreverdata: 1257384751,
            }, MarketOffersWizard());

            const links = await bro.$$(MarketOffersWizard.firstProductCard.links());
            assert.lengthOf(links, 5, 'должно быть 5 ссылок');

            for (const link of links) {
                const className = await link.getAttribute('class');
                const { searchParams: paramsBefore } = new URL(await link.getAttribute('href'));
                assert.isTrue(paramsBefore.has('rs'), `до клика в ссылке '${className}' нет параметра rs`);
                assert.isFalse(paramsBefore.has('parent-reqid'), `до клика в ссылке '${className}' есть параметр parent-reqid`);
                assert.isFalse(paramsBefore.has('utm_source_service'), `до клика в ссылке '${className}' есть параметр utm_source_service`);

                if (className.includes('ProductCard-LinkOverlay')) {
                    // Если нужно кликнуть по ссылке, накрывающей всю информацию под картинкой,
                    // то кликаем в лейбл лучшей цены, чтобы не попасть в другие ссылки.
                    await bro.yaClickAtTheMiddle(MarketOffersWizard.firstProductCard.bestPrice());
                } else {
                    await link.click();
                }

                const { searchParams: paramsAfter } = new URL(await link.getAttribute('href'));
                assert.isTrue(paramsAfter.has('parent-reqid'), `после клика в ссылке '${className}' нет параметра parent-reqid`);
                assert.isTrue(paramsAfter.has('utm_source_service'), `после клика в ссылке '${className}' нет параметра utm_source_service`);
            }
        });
    });

    hermione.only.in(['chrome-desktop', 'iphone']);
    it('Доступность', async function() {
        const { browser } = this;

        await browser.yaOpenSerp({
            foreverdata: 3356472073,
            exp_flags: ['a11y_validate=1'],
        }, MarketOffersWizard());

        await browser.yaCheckElementA11y(MarketOffersWizard());
    });

    it('Не рендерить сниппет при отсутствии контента внутри в центральной колонке', async function() {
        await this.browser.yaOpenSerp({
            foreverdata: 3711302451,
        }, '.main');
        const found = await this.browser.$$(MarketOffersWizard());
        assert.lengthOf(found, 0, 'Отрисовался сниппет врезки без предложений внутри');
    });
});
