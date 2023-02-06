import {makeSuite, mergeSuites, prepareSuite} from 'ginny';

import {userMenuSuite} from '@self/root/src/spec/hermione/test-suites/blocks/growingCashback/userMenuSuites';

export default prepareState => makeSuite('Пункт меню "Растущий кешбэк".', {
    environment: 'kadavr',
    issue: 'MARKETFRONT-70243',
    feature: 'Растущий кешбэк',
    id: 'marketfront-5295',
    story: mergeSuites({
        'Акция доступна': {
            'По умолчанию': prepareSuite(userMenuSuite, {
                params: {
                    shouldBeShown: true,
                    isPromoAvailable: true,
                    isGotFullReward: false,
                    prepareState,
                },
            }),
            'Пользователь набрал максимальное количество баллов': prepareSuite(userMenuSuite, {
                params: {
                    shouldBeShown: false,
                    isPromoAvailable: true,
                    isGotFullReward: true,
                    prepareState,
                },
            }),
        },
        'Акция не доступна': prepareSuite(userMenuSuite, {
            params: {
                shouldBeShown: false,
                isPromoAvailable: false,
                isGotFullReward: false,
                prepareState,
            },
        }),
    }),
});
