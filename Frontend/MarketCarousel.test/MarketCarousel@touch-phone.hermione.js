'use strict';

const { Page, MarketCarousel, MarketCarouselHitCounter, ExtralinksPopup } = require('../../../Market.test/Market.page-object');
const { productCardTooltip } = require('../../../../../components/ProductCard/ProductCard.test/ProductCard.page-object/index@common');

const checkBaobabEventIdInUrl = (url, baobabPath = '') => {
    const baobabEventId = new URL(url).searchParams.get('baobab_event_id');

    return assert(baobabEventId !== null, `В ссылку ${baobabPath ? `для '${baobabPath}' ` : ''}не добавился параметр baobab_event_id`);
};

specs({
    feature: 'Товарная галерея',
}, () => {
    const query = 'iphone';

    hermione.also.in('iphone-dark');
    hermione.only.in(['chrome-phone', 'iphone-dark']);
    describe('Экстралинки', async function() {
        beforeEach(async function() {
            await this.browser.yaOpenSerp({
                foreverdata: 2055513564,
            }, MarketCarousel());

            await this.browser.click(MarketCarousel.extralinks());
            await this.browser.yaWaitForVisible(ExtralinksPopup());
        });

        it('Выпадающее меню', async function() {
            const links = await this.browser.$$(ExtralinksPopup.link());
            assert.lengthOf(links, 2, 'в экстралинках должно быть две ссылки');
            await this.browser.assertView('extralinks', ExtralinksPopup(), {
                captureElementFromTop: false,
                allowViewportOverflow: true,
            });
        });

        it('Пожаловаться', async function() {
            const feedback = await this.browser.$(ExtralinksPopup.firstLink());

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
                'https://yandex.ru/support/direct/dynamic-text-ads/product-gallery.html',
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
    });

    describe('По типу карточки', function() {
        it('Оферы Директа', async function() {
            await this.browser.yaOpenSerp({
                foreverdata: 511054513,
            }, MarketCarousel());

            await this.browser.assertView('direct-offers', MarketCarousel());
        });

        hermione.also.in('chrome-grid-480');
        hermione.only.in(['chrome-grid-480']);
        it('Лейбл слева от тайтла в карусели', async function() {
            await this.browser.yaOpenSerp({
                foreverdata: 511054513,
            }, MarketCarousel());

            await this.browser.assertView('direct-lable-right', MarketCarousel());
        });

        hermione.also.in('iphone-dark');
        it('Оферы Маркета', async function() {
            await this.browser.yaOpenSerp({
                foreverdata: 3943098408,
            }, MarketCarousel());

            await this.browser.assertView('market-offers', MarketCarousel());
        });
        it('Старая цена в офере директа', async function() {
            await this.browser.yaOpenSerp({
                foreverdata: 3449572634,
            }, MarketCarousel());

            await this.browser.assertView('direct-offers-old-price', MarketCarousel.productCard());
        });
        hermione.also.in('iphone-dark');
        it('Оферы Маркета. Внешний вид с дисклеймерами', async function() {
            await this.browser.yaOpenSerp({
                foreverdata: 2009316186,
            }, MarketCarousel());

            await this.browser.assertView('market-offers-with-disclaimers', MarketCarousel());
        });
        it('Неявные модели Маркета', async function() {
            await this.browser.yaOpenSerp({
                text: query,
                foreverdata: 3113079982,
            }, MarketCarousel());

            await this.browser.assertView('market-implicit-models', MarketCarousel());
        });
        it('Модели Маркета', async function() {
            await this.browser.yaOpenSerp({
                foreverdata: 3147147520,
            }, MarketCarousel());

            await this.browser.assertView('market-models', MarketCarousel());
        });
        it('Офер Директа + Оферы Маркета', async function() {
            await this.browser.yaOpenSerp({
                foreverdata: 3363883224,
            }, MarketCarousel());

            await this.browser.assertView('direct-offers', MarketCarousel());
        });

        it('Карусель должна отображаться, если пришло 2 директовых оффера без маркетных офферов', async function() {
            await this.browser.yaOpenSerp({
                foreverdata: 156200853,
            }, MarketCarousel());

            await this.browser.assertView('direct-min-offers', MarketCarousel());
        });

        it('Карусель не должна отображаться, если пришел 1 директовый оффер без маркетных офферов', async function() {
            await this.browser.yaOpenSerp({
                foreverdata: 3361771829,
            }, Page());

            await this.browser.yaShouldNotExist(MarketCarousel());
        });

        it('Декодирование спец символов в названии директового оффера', async function() {
            await this.browser.yaOpenSerp({
                foreverdata: 3617967145,
            }, MarketCarousel());

            await this.browser.assertView('direct-offer-decoded', MarketCarousel.productCard());
        });
    });

    describe('Неявная', function() {
        it('Проверка счетчиков', async function() {
            await this.browser.yaOpenSerp({
                foreverdata: 3316108464,
            }, MarketCarousel());

            await this.browser.yaScrollContainer(MarketCarousel.scroller.wrap(), 9999, 0);

            const linkOverlaySelector = MarketCarousel.productCard.linkOverlay();
            // поднимаем overlay выше тумбы (zIndex: 20)
            await this.browser.execute(function(selector) {
                window.$(selector).css('zIndex', '21');
            }, linkOverlaySelector);
            await this.browser.yaMoveAndClick(linkOverlaySelector, 0, 0);
            const urlAfterClick = await this.browser.getAttribute(linkOverlaySelector, 'href');
            checkBaobabEventIdInUrl(urlAfterClick, 'linkOverlay');

            // опускаем overlay под уровень остальных ссылок
            await this.browser.execute(function(selector) {
                window.$(selector).css('zIndex', '0');
            }, linkOverlaySelector);

            for (const { selector, baobabPath } of [
                { selector: MarketCarousel.productCard.thumb(), baobabPath: 'thumb' },
                { selector: MarketCarousel.productCard.title.link(), baobabPath: 'title' },
                { selector: MarketCarousel.productCard.greenUrl.link(), baobabPath: 'greenUrl' },
                { selector: MarketCarousel.productCard.price.link(), baobabPath: 'price' },
            ]) {
                await this.browser.yaCheckBaobabCounter(
                    selector,
                    // Тут надо разобраться с $top
                    // https://a.yandex-team.ru/arc_vcs/frontend/projects/web4/blocks-common/main/__carousel/main__carousel.priv.js#L44
                    { path: `//$result/marketCarousel/scroller/productCard/${baobabPath}` },
                );
                const urlAfterClick = await this.browser.getAttribute(selector, 'href');
                checkBaobabEventIdInUrl(urlAfterClick, baobabPath);
            }
        });
    });

    it('Проверка атрибутов логирования', async function() {
        const { browser } = this;

        await browser.yaOpenSerp({
            foreverdata: 3617967145,
        }, MarketCarousel());

        await browser.yaCheckBaobabServerCounter({
            // Тут надо разобраться с $top
            // https://a.yandex-team.ru/arc_vcs/frontend/projects/web4/blocks-common/main/__carousel/main__carousel.priv.js#L44
            path: '//$result/marketCarousel/scroller/productCard[@type="adv" and @title]',
        });
    });

    describe('Подтверждение видимости на клиенте', function() {
        hermione.only.in('chrome-phone');
        it('prefetch=0', async function() {
            const { browser } = this;

            await browser.yaOpenSerp({
                text: 'foreverdata',
                foreverdata: 1191664293,
            }, Page());

            await this.browser.yaShouldNotExist(MarketCarousel.serpBkCounter());
        });

        hermione.only.in('searchapp-phone');
        it('prefetch=1', async function() {
            const { browser } = this;

            await browser.yaOpenSerp({
                text: 'foreverdata',
                foreverdata: 1191664293,
                prefetch: 1,
            }, Page());

            await this.browser.yaShouldNotBeVisible(MarketCarousel.serpBkCounter());

            const hitCounter = await this.browser.execute(function(selector) {
                const serpBkCounters = document.querySelectorAll(selector);

                if (!serpBkCounters || !serpBkCounters.length) return `по селектору ${selector} ничего не найдено`;

                if (
                    serpBkCounters[0].textContent !== 'bk848484(https://yabs.yandex.ru/hitcount)'
                ) return 'hitcount не найден';

                return true;
            }, MarketCarouselHitCounter());

            assert(hitCounter === true, hitCounter);

            const visibilityCounters = await this.browser.execute(function(selector) {
                const serpBkCounters = document.querySelectorAll(selector);

                if (!serpBkCounters || !serpBkCounters.length) return `по селектору ${selector} ничего не найдено`;

                if (serpBkCounters.length !== 3) return `по селектору ${selector} должно быть найдено 3 элемента`;

                if (
                    serpBkCounters[0].textContent !== 'bk848484(https://yabs.yandex.ru/count/LINKHEAD~1=LINKTAIL~1)'
                ) return 'счетчик видимости для первой карточки не найден';

                if (
                    serpBkCounters[1].textContent !== 'bk848484(https://yabs.yandex.ru/count/LINKHEAD~1=LINKTAIL~2)'
                ) return 'счетчик видимости для второй карточки не найден';

                if (
                    serpBkCounters[2].textContent !== 'bk848484(https://yabs.yandex.ru/count/LINKHEAD~1=LINKTAIL~3)'
                ) return 'счетчик видимости для третьей карточки не найден';

                return true;
            }, MarketCarousel.serpBkCounter());

            assert(visibilityCounters === true, visibilityCounters);
        });
    });

    describe('С дисклеймером', async function() {
        it('Дисклеймер в тултипе в гринурле', async function() {
            await this.browser.yaOpenSerp({
                foreverdata: 1130659148,
            }, MarketCarousel());

            // Иначе шапка вызывает закрытие попапов
            // Раздебажить не удалось
            await this.browser.yaHideHeader();

            await this.browser.assertView('greenurl-disclaimer', MarketCarousel.productCard());
            await this.browser.yaCheckBaobabCounter(MarketCarousel.productCard.greenUrl.disclaimer(), {
                path: '/$page/$main/$result/marketCarousel/scroller/productCard/greenUrlDisclaimer',
            });
            await this.browser.assertView('greenurl-disclaimer-tooltip', MarketCarousel());
            await this.browser.yaTouch(MarketCarousel.productCard.greenUrl.disclaimer());
            await this.browser.yaShouldNotBeVisible(productCardTooltip());
            await this.browser.yaTouch(MarketCarousel.productCard.greenUrl.disclaimer());
            await this.browser.yaShouldBeVisible(productCardTooltip());
            await this.browser.scroll(0, 100);
            await this.browser.yaShouldNotBeVisible(productCardTooltip());
        });

        it('Работает на технологиях Яндекса', async function() {
            await this.browser.yaOpenSerp({
                foreverdata: 3585820438,
            }, MarketCarousel());

            await this.browser.assertView('gallery-item-business-unit', MarketCarousel.productCard());
            await this.browser.yaCheckBaobabCounter(MarketCarousel.productCard.greenUrl.disclaimer(), {
                path: '/$page/$main/$result/marketCarousel/scroller/productCard/greenUrlBusinessUint',
            });
            await this.browser.assertView('gallery-greenurl-business-unit-tooltip', MarketCarousel());
            await this.browser.yaTouch(MarketCarousel.productCard.greenUrl.disclaimer());
            await this.browser.yaShouldNotBeVisible(productCardTooltip());
            await this.browser.yaTouch(MarketCarousel.productCard.greenUrl.disclaimer());
            await this.browser.yaShouldBeVisible(productCardTooltip());
            await this.browser.scroll(0, 100);
            await this.browser.yaShouldNotBeVisible(productCardTooltip());
        });
    });
});
