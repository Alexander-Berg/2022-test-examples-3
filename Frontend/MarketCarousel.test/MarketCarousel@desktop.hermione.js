'use strict';

const { Page, MarketCarousel } = require('../../../Market.test/Market.page-object');
const { button } = require('../../../../../components/Button/Button.test/Button.page-object/index@common');

MarketCarousel.productCard.button = button.copy();

const checkBaobabEventIdInUrl = (url, baobabPath = '') => {
    const baobabEventId = new URL(url).searchParams.get('baobab_event_id');

    return assert(baobabEventId !== null, `В ссылку ${baobabPath ? `для '${baobabPath}' ` : ''}не добавился параметр baobab_event_id`);
};

specs({
    feature: 'Товарная галерея',
}, () => {
    describe('По типу карточки', function() {
        describe('Офферы директа', function() {
            it('Карусель должна отображаться, если пришло больше 2-х офферов Директа без маркетных офферов', async function() {
                await this.browser.yaOpenSerp({
                    foreverdata: 2055513564,
                }, MarketCarousel());

                await this.browser.assertView('direct-offers', MarketCarousel());
            });

            it('Карусель не должна отображаться, когда пришло 2 директовых оффера без маркетных офферов', async function() {
                await this.browser.yaOpenSerp({
                    foreverdata: 156200853,
                }, Page());

                await this.browser.yaShouldNotExist(MarketCarousel());
            });

            it('Декодирование спец символов в названии директового оффера', async function() {
                await this.browser.yaOpenSerp({
                    foreverdata: 3617967145,
                }, MarketCarousel());

                await this.browser.assertView('direct-offer-decoded', MarketCarousel.productCard());
            });

            it('Поддерржка старой цены', async function() {
                await this.browser.yaOpenSerp({
                    foreverdata: 3449572634,
                }, MarketCarousel());

                await this.browser.assertView('direct-offer-old-price', MarketCarousel.productCard());
            });
        });

        describe('Офферы Маркета', function() {
            it('Карусель должна отображаться для офферов маркета', async function() {
                await this.browser.yaOpenSerp({
                    foreverdata: 499880711,
                }, MarketCarousel());

                await this.browser.assertView('market-offers', MarketCarousel());
            });

            it('Внешний вид с дисклеймерами', async function() {
                await this.browser.yaOpenSerp({
                    foreverdata: 3282771238,
                }, MarketCarousel());

                await this.browser.assertView('market-offers-with-disclaimers', MarketCarousel());
            });
        });

        describe('Модельная', function() {
            it('Карусель должна отображаться для моделей Маркета', async function() {
                await this.browser.yaOpenSerp({
                    foreverdata: 2562962663,
                }, MarketCarousel());

                await this.browser.assertView('market-models', MarketCarousel());
            });
        });

        describe('Неявная', function() {
            it('Карусель не должна отображаться, когда пришло меньше 3-х офферов директа без маркетных офферов', async function() {
                await this.browser.yaOpenSerp({
                    foreverdata: 1261002851,
                }, Page());

                await this.browser.yaShouldNotExist(MarketCarousel());
            });

            it('Карусель должна отображаться, когда пришло 3 оффера директа без маркетных офферов', async function() {
                await this.browser.yaOpenSerp({
                    foreverdata: 3561208592,
                }, MarketCarousel());

                await this.browser.assertView('direct-implicit-models', MarketCarousel());
            });

            it('Карусель должна отображаться для неявных моделей Маркета', async function() {
                await this.browser.yaOpenSerp({
                    foreverdata: 1073933814,
                }, MarketCarousel());

                await this.browser.assertView('market-implicit-models', MarketCarousel());
            });

            it('Если есть товары маркета, то должно быть максимум два товара Директа', async function() {
                await this.browser.yaOpenSerp({
                    foreverdata: 3316108464,
                }, MarketCarousel());

                await this.browser.assertView('market-implicit-models-max-direct-offers', MarketCarousel());
            });

            it('Проверка счетчиков', async function() {
                await this.browser.yaOpenSerp({
                    foreverdata: 3316108464,
                }, MarketCarousel());

                await this.browser.yaScrollContainer(MarketCarousel.scroller.wrap(), 9999, 0);

                const linkOverlaySelector = MarketCarousel.productCard.linkOverlay();
                // Сначала наведем на карточку, чтобы увеличить область LinkOverlay и не кликнуть в Thumb
                await this.browser.moveToObject(MarketCarousel.productCard(), 1, 1);
                await this.browser.yaMoveAndClick(linkOverlaySelector, 1, 1);
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
                        { path: `/$page/$top/$result/marketCarousel/scroller/productCard/${baobabPath}` },
                    );
                    const urlAfterClick = await this.browser.getAttribute(selector, 'href');
                    checkBaobabEventIdInUrl(urlAfterClick, baobabPath);
                }
            });
        });

        describe('Офер Директа + Оферы Маркета', function() {
            it('Карусель должна отображать оферы Директа и офферы Маркета', async function() {
                await this.browser.yaOpenSerp({
                    foreverdata: 4167271971,
                }, MarketCarousel());

                await this.browser.assertView('market-direct-offers', MarketCarousel());
            });

            it('Проверка внешнего вида офферов без картинок', async function() {
                await this.browser.yaOpenSerp({
                    foreverdata: 4157967411,
                }, MarketCarousel());

                await this.browser.assertView('market-direct-offers-without-images', MarketCarousel());
            });
        });
    });

    describe('Реакция при наведении курсора мыши', function() {
        it('Базовые проверки', async function() {
            await this.browser.yaOpenSerp({
                foreverdata: 2410713847,
            }, MarketCarousel());

            await this.browser.assertView('MarketCarousel-before-hover', [MarketCarousel.productCard(), MarketCarousel.secondProductCard()]);

            await this.browser.moveToObject(MarketCarousel.productCard());
            await this.browser.assertView('MarketCarousel-first-after-hover', MarketCarousel.productCard());

            await this.browser.moveToObject(MarketCarousel.secondProductCard());
            await this.browser.assertView('MarketCarousel-second-after-hover', MarketCarousel.secondProductCard());
        });
    });

    it('Проверка атрибутов логирования', async function() {
        const { browser } = this;
        const baobabPath = '/$page/$top/$result/marketCarousel/scroller/productCard';
        const baobabAdvLogNode = {
            path: baobabPath,
            attrs: {
                type: 'adv',
                title: 'Черные «кроссовки» NIKE Air Zoom Winflo 5',
            },
        };

        await browser.yaOpenSerp({
            foreverdata: 3617967145,
        }, MarketCarousel());

        await browser.yaCheckBaobabServerCounter(baobabAdvLogNode);
    });

    describe('С дисклеймером', async function() {
        it('Дисклеймер в тултипе в гринурле', async function() {
            await this.browser.yaOpenSerp({
                foreverdata: 1130659148,
            }, MarketCarousel());

            await this.browser.assertView('greenurl-disclaimer', MarketCarousel.productCard());
            await this.browser.moveToObject(MarketCarousel.productCard.greenUrl.disclaimer());
            await this.browser.yaAssertViewExtended('greenurl-disclaimer-tooltip', MarketCarousel(), { verticalOffset: 30 });
        });

        it('Работает на технологиях Яндекса', async function() {
            await this.browser.yaOpenSerp({
                foreverdata: 3585820438,
            }, Page());

            await this.browser.assertView('gallery-item-business-unit', MarketCarousel.productCard());
            await this.browser.moveToObject(MarketCarousel.productCard.greenUrl.disclaimer());
            await this.browser.yaAssertViewExtended('gallery-greenurl-business-unit-tooltip', MarketCarousel(), { verticalOffset: 30 });
        });
    });
});
