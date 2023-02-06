import {makeCase, makeSuite, mergeSuites} from 'ginny';
import {createOffer, mergeState} from '@yandex-market/kadavr/mocks/Report/helpers';
import deliveryConditionMock from '@self/root/src/spec/hermione/kadavr-mock/deliveryCondition/deliveryCondition';
import {yandexPlusPerk} from '@self/root/src/spec/hermione/kadavr-mock/loyalty/perks';
import {profiles} from '@self/project/src/spec/hermione/configs/profiles';
import FreeDeliveryWithPlusLink from '@self/root/src/components/FreeDeliveryWithPlusLink/__pageObject';
import YaPlusPopupContent from '@self/root/src/components/YaPlusPopupContent/__pageObject';
import Title from '@self/root/src/uikit/components/Title/__pageObject';
import Text from '@self/root/src/uikit/components/Text/__pageObject';
import LinkMarket from '@self/root/src/components/CommonLink/__pageObject';
import LinkRoot from '@self/root/src/components/Link/__pageObject';
import {FREE_DELIVERY_POPUP_AUTO_OPEN} from '@self/root/src/constants/queryParams';
import {cpaOfferMock} from '@self/project/src/spec/hermione/fixtures/offer/cpaOffer';
import productWithCPADO from '@self/project/src/spec/hermione/fixtures/product/productWithCPADO';

// Переодически при составление стейта для кадаврика deliveryConditionMock портится и тесты флапают с ошибкой Cannot read properties of undefined (reading 'threshold')
const THRESHOLD = deliveryConditionMock?.special?.yaPlus?.threshold?.value ?? 699;

const YANDEX_HELP_COOKIE = 'yandex_help';

/**
 * Отличия:
 * - beforeEach (стейт кадавра, авторизация)
 * - добавлен afterEach, id из testpalm
 * - установка перка Плюсовика из теста, а не из плагина при авторизации
 * - PageObject Link (тут маркетный, не из @self/root), искользуется в кейсе незалогина + урл для авторизации
 *
 * Можно пробовать уницифировать и уносить в @self/root, но сейчас времени нет
 */
module.exports = makeSuite('Возможность бесплатной доставки с Яндекс.Плюс.', {
    params: {
        pageId: 'Страница которую нужно открыть, оличается в десктопе и таче',
        hasYaPlus: 'Пользователь с подпиской Плюса',
        freeDeliveryPopupAutoOpen: 'Параметр страницы для автооткрытия попапа беслпатной доставки',
    },
    defaultParams: {
        hasYaPlus: 0,
        freeDeliveryPopupAutoOpen: 0,
    },
    feature: 'Выгода Плюса',
    environment: 'kadavr',
    issue: 'MARKETFRONT-38507',
    story: mergeSuites(
        {
            async beforeEach() {
                this.setPageObjects({
                    yaPlusPopupContent: () => this.createPageObject(YaPlusPopupContent, {
                        parent: this.yandexPlusFreeDeliveryPopup,
                        root: `${YaPlusPopupContent.root}${YaPlusPopupContent.yaPlusFreeDeliveryRoot}`,
                    }),
                    yaPlusPopupTitle: () => this.createPageObject(Title, {
                        parent: this.yaPlusPopupContent,
                    }),
                    yaPlusPopupText: () => this.createPageObject(Text, {
                        parent: this.yaPlusPopupContent,
                    }),
                    yaPlusPopupButton: () => this.createPageObject(LinkRoot, {
                        parent: this.yaPlusPopupContent,
                    }),
                });

                if (this.params.isAuth) {
                    const profile = profiles['pan-topinambur'];

                    await this.browser.yaLogin(profile.login, profile.password);
                }

                if (this.params.hasYaPlus) {
                    await this.browser.setState('Loyalty.collections.perks', [yandexPlusPerk]);
                }

                let pageParams;
                let reportState;

                const localOffer = createOffer({
                    ...cpaOfferMock,
                    delivery: {
                        ...cpaOfferMock.delivery,
                        betterWithPlus: true,
                    },
                }, productWithCPADO.offerMock.wareId);

                if (this.params.pageId === 'touch:offer' || this.params.pageId === 'market:offer') {
                    reportState = mergeState([localOffer, {
                        data: {
                            blueTariffs: deliveryConditionMock,
                        },
                    }]);
                    pageParams = {
                        offerId: productWithCPADO.offerMock.wareId,
                    };
                }

                if (this.params.pageId === 'touch:product' || this.params.pageId === 'market:product') {
                    reportState = mergeState([
                        productWithCPADO.state,
                        localOffer,
                        {
                            data: {
                                search: {
                                    total: 1,
                                    totalOffers: 1,
                                },
                                blueTariffs: deliveryConditionMock,
                            },
                        },
                    ]);
                    pageParams = {...productWithCPADO.route};
                }

                await this.browser.setState('Carter.items', []);
                await this.browser.setState('report', reportState);

                if (this.params.freeDeliveryPopupAutoOpen) {
                    pageParams[FREE_DELIVERY_POPUP_AUTO_OPEN] = 1;
                }

                // Ставим куку что бы попап онбординга Помощи рядом не открывался и не загораживал попап нотификации
                await this.browser.yaSetCookie({
                    name: YANDEX_HELP_COOKIE,
                    value: '1',
                });

                await this.browser.yaOpenPage(this.params.pageId, pageParams);
            },
            afterEach() {
                if (this.params.isAuth) {
                    return this.browser.yaLogout();
                }
            },
        },
        makeSuite('Авторизованный пользователь.', {
            defaultParams: {
                isAuthWithPlugin: true,
            },
            story: {
                'Ссылка "C Яндекс Плюс доставка бесплатная" отображается и по клику открывает попап': makeCase({
                    id: 'marketfront-4558',
                    async test() {
                        await this.freeDeliveryWithPlusLink.isVisible().should.eventually.be.equal(
                            true,
                            'Ссылка "C Яндекс Плюс доставка бесплатная" должна отображаться'
                        );

                        await this.freeDeliveryWithPlusLink.getText().should.eventually.be.equal(
                            'C Яндекс Плюс доставка бесплатная',
                            'Ссылка "C Яндекс Плюс доставка бесплатная" должна содержать корректный текст'
                        );

                        await this.freeDeliveryWithPlusLink.click();
                        await this.yaPlusPopupContent.waitForVisible();

                        return this.yaPlusPopupContent.isVisible().should.eventually.be.equal(
                            true,
                            'Попап с информацией о бесплатной доставке должен отобажаться'
                        );
                    },
                }),
                'Попап бесплатной доставки отображается с корректным контентом для НЕплюсовика': makeCase({
                    id: 'marketfront-4558',
                    async test() {
                        await this.freeDeliveryWithPlusLink.click();

                        const buttonLink = `https:${await this.browser.yaBuildURL(
                            'external:ya-plus', {
                                utm_source: 'market',
                                utm_medium: 'banner',
                                utm_campaign: 'MSCAMP-77',
                                utm_term: 'src_market',
                                utm_content: 'free_delivery_with_plus_popup',
                            })}`;

                        return checkFreeDeliveryPopupContent.call(this, {
                            title: `Бесплатно привезём\nзаказы от ${THRESHOLD} ₽`,
                            text: 'Если у вас подключен Плюс',
                            buttonText: 'Пойду подключу',
                            buttonLink,
                        });
                    },
                }),
                /**
                 * Это кейс только на случай, если попап был открыт после авторизации через автооткрытие, т.к.
                 *  для Плюсовика в интерфейсе ссылка нп попап не показывается.
                 */
                'Попап бесплатной доставки отображается с корректным контентом для Плюсовика': makeCase({
                    defaultParams: {
                        freeDeliveryPopupAutoOpen: 1,
                        hasYaPlus: 1,
                    },
                    id: 'marketfront-4644',
                    async test() {
                        return checkFreeDeliveryPopupContent.call(this, {
                            title: 'За доставку\nвам платить не нужно',
                            text: `Мы бесплатно привезём\nнекрупногабаритные заказы от ${THRESHOLD} ₽`,
                            buttonText: 'Замечательно',
                        });
                    },
                }),
                'Автооткрытие попапа. Попап открывается автоматически, если передан параметр': makeCase({
                    defaultParams: {
                        freeDeliveryPopupAutoOpen: 1,
                    },
                    id: 'marketfront-4559',
                    async test() {
                        await this.yaPlusPopupContent.waitForVisible();

                        return this.yaPlusPopupContent.isVisible().should.eventually.be.equal(
                            true,
                            'Попап с информацией о бесплатной доставке должен отобажаться'
                        );
                    },
                }),
            },
        }),
        makeSuite('Неавторизованный пользователь.', {
            defaultParams: {
                isAuth: false,
            },
            id: 'marketfront-4559',
            story: {
                ['Ссылка "C Яндекс Плюс доставка бесплатная" отображается'
                    + ' и ведет на авторизацию c возвратом на текущую страницу со спец.параметром']:
                makeCase({
                    async test() {
                        // переопределяем PageObject ссылки (т.к. в данном случае это Link)
                        this.setPageObjects({
                            freeDeliveryWithPlusLink: () => this.createPageObject(LinkMarket, {
                                root: `${FreeDeliveryWithPlusLink.root}${LinkMarket.root}`,
                            }),
                        });

                        await this.freeDeliveryWithPlusLink.isVisible().should.eventually.be.equal(
                            true,
                            'Ссылка "C Яндекс Плюс доставка бесплатная" должна отображаться'
                        );

                        await this.freeDeliveryWithPlusLink.getText().should.eventually.be.equal(
                            'C Яндекс Плюс доставка бесплатная',
                            'Ссылка "C Яндекс Плюс доставка бесплатная" должна содержать корректный текст'
                        );

                        const currentUrl = await this.browser.getUrl();

                        /**
                         * Параметр shopId добавляется только в десктопе где-то на этапе формирование retpath.
                         * Для данной проверки никакой логики не несет.
                         */
                        const isOfferDesktopPage = this.params.pageId === 'market:offer';
                        const expectedRetpath = isOfferDesktopPage
                            ? `${currentUrl}?shopId=${cpaOfferMock.shop.id}&${FREE_DELIVERY_POPUP_AUTO_OPEN}=1`
                            : `${currentUrl}?${FREE_DELIVERY_POPUP_AUTO_OPEN}=1`;

                        return this.freeDeliveryWithPlusLink.getHref().should.eventually.be.link(
                            {
                                pathname: '/auth',
                                query: {
                                    retpath: expectedRetpath,
                                },
                            }, {
                                mode: 'equal',
                                skipProtocol: true,
                                skipHostname: true,
                            },
                            `Ссылка должна вести на авторизацию с возвратом на текущую страницу с параметром ${FREE_DELIVERY_POPUP_AUTO_OPEN}=1`
                        );
                    },
                }),
            },
        })
    ),
});

async function checkFreeDeliveryPopupContent({title, text, buttonText, buttonLink}) {
    await this.yaPlusPopupContent.waitForVisible();

    await this.yaPlusPopupContent.isVisible().should.eventually.be.equal(
        true,
        'Попап с информацией о бесплатной доставке должен отображаться'
    );

    await this.yaPlusPopupTitle.getTitle().should.eventually.be.equal(
        title,
        'Попап с информацией о бесплатной доставке должен содержать корректный заголовок'
    );

    await this.yaPlusPopupText.getText().should.eventually.be.equal(
        text,
        'Попап с информацией о бесплатной доставке должен содержать корректный заголовок'
    );

    await this.yaPlusPopupButton.getText().should.eventually.be.equal(
        buttonText,
        'Попап с информацией о бесплатной доставке должен содержать корректный заголовок'
    );

    /**
     * Кнопка в попапе - ссылка, проверяем ее корректность.
     * Дополнительно проверяет корректность передачи параметров для аналитики.
     */
    if (buttonLink) {
        const tabIds = await this.browser.getTabIds();
        await this.yaPlusPopupButton.click();
        const newTabId = await this.browser.yaWaitForNewTab({startTabIds: tabIds});

        return this.allure.runStep('Переключаемся на новую вкладку', async () => {
            await this.browser.switchTab(newTabId);
            const url = await this.browser.getUrl();
            await this.expect(url).to.be.link(buttonLink, {
                mode: 'match',
            });

            // закрыть новую вкладку
            await this.browser.close();
        });
    }

    /**
     * Кнопка в попапе - НЕ ссылка, значит действие.
     * В данном случае кнопка всешда закрывает попап, проверяем это.
     */
    await this.browser.allure.runStep('Кликаем по кнопке попапа', () => this.yaPlusPopupButton.click());
    await this.yaPlusPopupContent.waitForHidden(5000);

    return this.yaPlusPopupContent.isVisible().should.eventually.be.equal(
        false,
        'Попап с информацией о бесплатной доставке должен закрыться после клика на кнопку'
    );
}
