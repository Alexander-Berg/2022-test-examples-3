import {makeSuite, prepareSuite, mergeSuites} from 'ginny';
import {createStories} from '@self/platform/spec/hermione/helpers/createStories';
import dayjs from 'dayjs';
import ru from 'dayjs/locale/ru';
import {routes} from '@self/platform/spec/hermione/configs/routes';
import WithDeliveryTextSuite from '@self/platform/spec/hermione/test-suites/blocks/SearchOffer/withDeliveryTextSuite';
import EmptyDeliverySuite from '@self/platform/spec/hermione/test-suites/blocks/SearchOffer/emptyDeliverySuite';
import SearchSnippetDelivery from '@self/platform/spec/page-objects/containers/SearchSnippet/Delivery';
import {
    cpaType3POfferMock,
    cpaTypeDSBSOfferMock,
} from '@self/project/src/spec/gemini/fixtures/cpa/mocks/cpaOffer.mock';
import {offerMock as cpaOfferMock} from '@self/project/src/spec/hermione/fixtures/offer/offer';
import createProductWithCPADO from '@self/platform/spec/hermione/test-suites/tops/pages/product/fixtures/productWithCPADO';
import {region} from '@self/root/src/spec/hermione/configs/geo';

const createVisibleDeliverySuite = ({queryParams, meta, params, deliveryOptions, offerMock}) =>
    mergeSuites(
        {
            async beforeEach() {
                offerMock.delivery.isExpress = false;
                const state = createProductWithCPADO(offerMock, deliveryOptions);
                await this.browser.setState('report', state
                );
                return this.browser.yaOpenPage('touch:search', queryParams);
            },
        },
        params.deliveryText ? prepareSuite(WithDeliveryTextSuite, {
            meta,
            params,
            pageObjects: {
                deliveryInfo() {
                    return this.createPageObject(SearchSnippetDelivery);
                },
            },
        }) : prepareSuite(EmptyDeliverySuite, {
            meta,
            params,
            pageObjects: {
                deliveryInfo() {
                    return this.createPageObject(SearchSnippetDelivery);
                },
            },
        })
    );

export default makeSuite('СИС.', {
    story: {
        'КМ с FBY-оффером.': {
            'Москва.': createStories([
                {
                    description: 'Срок доставки завтра.',
                    queryParams: {...routes.search.default, lr: region['Москва']},
                    meta: {
                        id: 'm-touch-3652',
                        issue: 'MARKETFRONT-50563',
                    },
                    deliveryOptions: {
                        dayFrom: 0,
                        dayTo: 1,
                    },
                    offerMock: cpaType3POfferMock,
                    params: {
                        deliveryText: 'Завтра, от Яндекса',
                    },
                },
                {
                    description: 'Срок доставки > трешхолда ',
                    queryParams: {...routes.search.default, lr: region['Москва']},
                    meta: {
                        id: 'm-touch-3653',
                        issue: 'MARKETFRONT-50563',
                    },
                    deliveryOptions: {
                        dayFrom: 1,
                        dayTo: 5,
                    },
                    offerMock: cpaType3POfferMock,
                    params: {
                        deliveryText: 'Доставка от Яндекса',
                    },
                },
                {
                    description: 'Срок доставки сегодня. ',
                    queryParams: {...routes.search.default, lr: region['Москва']},
                    meta: {
                        id: 'm-touch-3654',
                        issue: 'MARKETFRONT-50563',
                    },
                    deliveryOptions: {
                        dayFrom: 0,
                        dayTo: 0,
                    },
                    offerMock: cpaType3POfferMock,
                    params: {
                        deliveryText: 'Сегодня, от Яндекса',
                    },
                },

            ], createVisibleDeliverySuite),
            'Город Миллионник. (Самара)': createStories([
                {
                    description: 'Срок доставки меньше трешхолда регионов, больше трешхолда Москвы',
                    queryParams: {...routes.search.default, lr: region['Самара']},
                    meta: {
                        id: 'm-touch-3655',
                        issue: 'MARKETFRONT-50563',
                    },
                    deliveryOptions: {
                        dayFrom: 2,
                        dayTo: 3,
                    },
                    offerMock: cpaType3POfferMock,
                    params: {
                        deliveryText: `${dayjs().add(3, 'day').locale(ru).format('D MMMM')}, от Яндекса`,
                    },
                },
                {
                    description: 'Срок доставки > трешхолда Миллионника',
                    queryParams: {...routes.search.default, lr: region['Самара']},
                    meta: {
                        id: 'm-touch-3656',
                        issue: 'MARKETFRONT-50563',
                    },
                    deliveryOptions: {
                        dayFrom: 2,
                        dayTo: 5,
                    },
                    offerMock: cpaType3POfferMock,
                    params: {
                        deliveryText: 'Доставка от Яндекса',
                    },
                },
            ], createVisibleDeliverySuite),
            'Регион (Иркутск)': createStories([
                {
                    description: 'Срок доставки < трешхолда регионов, больше трешхолда Москвы',
                    queryParams: {...routes.search.default, lr: region['Иркутск']},
                    meta: {
                        id: 'm-touch-3657',
                        issue: 'MARKETFRONT-50563',
                    },
                    deliveryOptions: {
                        dayFrom: 2,
                        dayTo: 5,
                    },
                    offerMock: cpaType3POfferMock,
                    params: {
                        deliveryText: `${dayjs().add(5, 'day').locale(ru).format('D MMMM')}, от Яндекса`,
                    },
                },
                {
                    description: 'Срок доставки > трешхолда регионов, больше трешхолда Москвы',
                    queryParams: {...routes.search.default, lr: region['Иркутск']},
                    meta: {
                        id: 'm-touch-3658',
                        issue: 'MARKETFRONT-50563',
                    },
                    deliveryOptions: {
                        dayFrom: 2,
                        dayTo: 7,
                    },
                    offerMock: cpaType3POfferMock,
                    params: {
                        deliveryText: 'Доставка от Яндекса',
                    },
                },
            ], createVisibleDeliverySuite),
        },
        'КМ с DSB-оффером.': {
            'Москва.': mergeSuites(
                createStories([
                    {
                        description: 'Срок доставки < трешхолда (доставят сегодня)',
                        queryParams: {...routes.search.default, lr: region['Москва']},
                        meta: {
                            id: 'm-touch-3659',
                            issue: 'MARKETFRONT-50563',
                        },
                        deliveryOptions: {
                            dayFrom: 0,
                            dayTo: 0,
                        },
                        offerMock: cpaTypeDSBSOfferMock,
                        params: {
                            deliveryText: 'Сегодня, от продавца',
                        },
                    },
                    {
                        description: 'Срок доставки < трешхолда (доставят завтра)',
                        queryParams: {...routes.search.default, lr: region['Москва']},
                        meta: {
                            id: 'm-touch-3660',
                            issue: 'MARKETFRONT-50563',
                        },
                        deliveryOptions: {
                            dayFrom: 0,
                            dayTo: 1,
                        },
                        offerMock: cpaTypeDSBSOfferMock,
                        params: {
                            deliveryText: 'Завтра, от продавца',
                        },
                    },
                    {
                        description: 'Срок доставки > трешхолда (5 дней)',
                        queryParams: {...routes.search.default, lr: region['Москва']},
                        meta: {
                            id: 'm-touch-3661',
                            issue: 'MARKETFRONT-50563',
                        },
                        deliveryOptions: {
                            dayFrom: 0,
                            dayTo: 5,
                        },
                        offerMock: cpaTypeDSBSOfferMock,
                        params: {
                            deliveryText: 'Доставка от продавца',
                        },
                    },
                ], createVisibleDeliverySuite)),
            'Город Миллионник.': mergeSuites(
                createStories([
                    {
                        description: 'Срок доставки меньше трешхолда регионов, больше трешхолда Москвы',
                        queryParams: {...routes.search.default, lr: region['Самара']},
                        meta: {
                            id: 'm-touch-3662',
                            issue: 'MARKETFRONT-50563',
                        },
                        deliveryOptions: {
                            dayFrom: 2,
                            dayTo: 3,
                        },
                        offerMock: cpaTypeDSBSOfferMock,
                        params: {
                            deliveryText: `${dayjs().add(3, 'day').locale(ru).format('D MMMM')}, от продавца`,
                        },
                    },
                    {
                        description: 'Срок доставки > трешхолда Миллионника',
                        queryParams: {...routes.search.default, lr: region['Самара']},
                        meta: {
                            id: 'm-touch-3663',
                            issue: 'MARKETFRONT-50563',
                        },
                        deliveryOptions: {
                            dayFrom: 2,
                            dayTo: 5,
                        },
                        offerMock: cpaTypeDSBSOfferMock,
                        params: {
                            deliveryText: 'Доставка от продавца',
                        },
                    },
                ], createVisibleDeliverySuite)),
            'Регион (Иркутск).': mergeSuites(
                createStories([
                    {
                        description: 'Срок доставки < трешхолда регионов, больше трешхолда Москвы',
                        queryParams: {...routes.search.default, lr: region['Иркутск']},
                        meta: {
                            id: 'm-touch-3668',
                            issue: 'MARKETFRONT-50563',
                        },
                        deliveryOptions: {
                            dayFrom: 2,
                            dayTo: 5,
                        },
                        offerMock: cpaTypeDSBSOfferMock,
                        params: {
                            deliveryText: `${dayjs().add(5, 'day').locale(ru).format('D MMMM')}, от продавца`,
                        },
                    },
                    {
                        description: 'Срок доставки > трешхолда регионов, больше трешхолда Москвы',
                        queryParams: {...routes.search.default, lr: region['Иркутск']},
                        meta: {
                            id: 'm-touch-3665',
                            issue: 'MARKETFRONT-50563',
                        },
                        deliveryOptions: {
                            dayFrom: 2,
                            dayTo: 7,
                        },
                        offerMock: cpaTypeDSBSOfferMock,
                        params: {
                            deliveryText: 'Доставка от продавца',
                        },
                    },
                ], createVisibleDeliverySuite)),
        },
        'КМ c CPC-оффером в ДО.': createStories([
            {
                description: 'Срок доставки < трешхолда (доставят сегодня)',
                queryParams: {...routes.search.default, lr: region['Москва']},
                meta: {
                    id: 'm-touch-3666',
                    issue: 'MARKETFRONT-50563',
                },
                deliveryOptions: {
                    dayFrom: 0,
                    dayTo: 0,
                },
                offerMock: cpaOfferMock,
                params: {
                    deliveryText: 'Сегодня, от продавца',
                },
            },
        ], createVisibleDeliverySuite),
    },
});
