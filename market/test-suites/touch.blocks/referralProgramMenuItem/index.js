import {makeSuite, mergeSuites, prepareSuite} from 'ginny';

import commonSuites from '@self/root/src/spec/hermione/test-suites/blocks/referralProgramMenuItem';
import userMenuSuite from '@self/root/src/spec/hermione/test-suites/blocks/referralProgramMenuItem/userMenuSuite';
import prepareState from '@self/root/src/spec/hermione/test-suites/blocks/referralProgramMenuItem/prepareState/';

export default makeSuite('Пункт меню "Приглашайте друзей".', {
    environment: 'kadavr',
    issue: 'MARKETFRONT-50643',
    feature: 'Реферальная программа',
    defaultParams: {
        isAuthWithPlugin: true,
    },
    story: mergeSuites({
        ...commonSuites,
        'Акция доступна, Неавторизованный пользователь,': prepareSuite(userMenuSuite({
            shouldBeShown: true,
        }), {
            meta: {
                id: 'marketfront-4833',
            },
            hooks: {
                async beforeEach() {
                    await this.browser.yaLogout();

                    await prepareState.call(this, {
                        isReferralProgramActive: true,
                        isGotFullReward: false,
                    });
                },
            },
        }),
    }),
});
