import {makeSuite, mergeSuites, prepareSuite} from 'ginny';

import {bannerSuite} from './bannerSuite';

export default makeSuite('Баннер "Растущий кешбэк".', {
    environment: 'kadavr',
    issue: 'MARKETFRONT-70827',
    feature: 'Растущий кешбэк',
    id: 'marketfront-5303',
    story: mergeSuites({
        'Акция доступна': {
            'По умолчанию': prepareSuite(bannerSuite, {
                params: {
                    shouldBeShown: true,
                    isPromoAvailable: true,
                    isGotFullReward: false,
                },
            }),
            'Не залогин': prepareSuite(bannerSuite, {
                params: {
                    shouldBeShown: false,
                    isPromoAvailable: true,
                    isGotFullReward: false,
                    isAuthWithPlugin: false,
                },
            }),
            'Пользователь набрал максимальное количество баллов': prepareSuite(bannerSuite, {
                params: {
                    shouldBeShown: false,
                    isPromoAvailable: true,
                    isGotFullReward: true,
                },
            }),
        },
        'Акция не доступна': prepareSuite(bannerSuite, {
            params: {
                shouldBeShown: false,
                isPromoAvailable: false,
                isGotFullReward: false,
            },
        }),
    }),
});
