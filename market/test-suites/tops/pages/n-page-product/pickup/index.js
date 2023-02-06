import {makeSuite, prepareSuite} from 'ginny';
import dayjs from 'dayjs';

// suites
import DeliveryPickupTextSuite from '@self/platform/spec/hermione/test-suites/blocks/n-delivery/pickupText';
// page-objects
import Delivery from '@self/platform/spec/page-objects/components/DeliveryInfo';
import DefaultOffer from '@self/platform/components/DefaultOffer/__pageObject';
// fixtures
import {ROUTE} from '@self/platform/spec/hermione/fixtures/product/common';
import pickupFreeTodayManyPointsDO from '@self/platform/spec/hermione/fixtures/pickup/free_today_manyPoints_do';
import pickupFreeTodayFewPoints from '@self/platform/spec/hermione/fixtures/pickup/free_today_fewPoints';
import pickup99rubIn10points from '@self/platform/spec/hermione/fixtures/pickup/99rub_4days_10points';
import pickupUnspecified from '@self/platform/spec/hermione/fixtures/pickup/unspecified';

const SMALL_REGION_ID = 63;

const getDayWithPretext = date => {
    const DAYS = ['воскресенье', 'понедельник', 'вторник', 'среду', 'четверг', 'пятницу', 'субботу'];
    const dayIndex = date.day();
    const day = DAYS[dayIndex];

    return `${dayIndex === 2 ? 'во' : 'в'} ${day}`;
};

export default makeSuite('Условия самовывоза на главной вкладке карточки модели', {
    environment: 'kadavr',
    story: {
        /*
        * Точка входа удалена. Задача на возврат тестов https://st.yandex-team.ru/MARKETFRONT-71298
        'Дефолтный оффер.': prepareSuite(DeliveryPickupMapLinkSuite, {
            meta: {
                id: 'marketfront-3796',
                issue: 'MARKETFRONT-5894',
            },
            hooks: {
                async beforeEach() {
                    await this.browser.setState(
                        'report',
                        pickupFreeTodayManyPointsDO.state
                    );

                    await this.browser.yaOpenPage(
                        'market:product',
                        ROUTE
                    );
                },
            },
            pageObjects: {
                delivery() {
                    return this.createPageObject(Delivery, {
                        parent: DefaultOffer.root,
                    });
                },
            },
            params: {
                slug: ROUTE.slug,
                productId: ROUTE.productId,
                fesh: BUSINESS_ID,
            },
        }),*/

        'Дефолтный оффер. Выгодные условия в городе-немиллионнике.': prepareSuite(DeliveryPickupTextSuite, {
            meta: {
                id: 'marketfront-3801',
                issue: 'MARKETFRONT-5991',
            },
            hooks: {
                async beforeEach() {
                    await this.browser.setState(
                        'report',
                        pickupFreeTodayFewPoints.state
                    );

                    await this.browser.yaOpenPage(
                        'market:product',
                        {
                            ...ROUTE,
                            lr: SMALL_REGION_ID,
                        }
                    );
                },

                afterEach() {
                    return this.browser.deleteCookie('lr');
                },
            },
            pageObjects: {
                delivery() {
                    return this.createPageObject(Delivery, {
                        parent: DefaultOffer.root,
                    });
                },
            },
            params: {
                expectedText: 'Самовывоз сегодня — бесплатно',
            },
        }),

        'Дефолтный оффер. Бесплатно сегодня.': prepareSuite(DeliveryPickupTextSuite, {
            meta: {
                id: 'marketfront-3795',
                issue: 'MARKETFRONT-5889',
            },
            hooks: {
                async beforeEach() {
                    await this.browser.setState(
                        'report',
                        pickupFreeTodayManyPointsDO.state
                    );

                    await this.browser.yaOpenPage(
                        'market:product',
                        ROUTE
                    );
                },
            },
            pageObjects: {
                delivery() {
                    return this.createPageObject(Delivery, {
                        parent: DefaultOffer.root,
                    });
                },
            },
            params: {
                expectedText: 'Самовывоз сегодня — бесплатно',
            },
        }),

        'Дефолтный оффер. Большой срок и цена 99 руб.': prepareSuite(DeliveryPickupTextSuite, {
            meta: {
                id: 'marketfront-3797',
                issue: 'MARKETFRONT-5915',
            },
            hooks: {
                async beforeEach() {
                    await this.browser.setState(
                        'report',
                        pickup99rubIn10points.state
                    );

                    await this.browser.yaOpenPage(
                        'market:product',
                        ROUTE
                    );
                },
            },
            pageObjects: {
                delivery() {
                    return this.createPageObject(Delivery, {
                        parent: DefaultOffer.root,
                    });
                },
            },
            params: {
                expectedText: `Самовывоз ${getDayWithPretext(dayjs().add(4, 'day'))}, ${dayjs().add(4, 'day').format('D MMMM')} — 99 ₽`,
            },
        }),

        'Дефолтный оффер. Есть самовывоз, но неизвестны условия.': prepareSuite(DeliveryPickupTextSuite, {
            meta: {
                id: 'marketfront-3802',
                issue: 'MARKETFRONT-5992',
            },
            hooks: {
                async beforeEach() {
                    await this.browser.setState(
                        'report',
                        pickupUnspecified.state
                    );

                    await this.browser.yaOpenPage(
                        'market:product',
                        ROUTE
                    );
                },
            },
            pageObjects: {
                delivery() {
                    return this.createPageObject(Delivery, {
                        parent: DefaultOffer.root,
                    });
                },
            },
            params: {
                expectedText: 'Есть самовывоз',
            },
        }),
    },
});
