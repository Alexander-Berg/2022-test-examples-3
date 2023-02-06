import {
    prepareSuite,
    makeSuite,
    mergeSuites,
} from 'ginny';

import {profiles} from '@self/project/src/spec/hermione/configs/profiles';
import bonusPageBonusCard from '@self/project/src/spec/hermione/test-suites/blocks/bonus/bonusCard';

// eslint-disable-next-line import/no-commonjs
module.exports = makeSuite('Мои купоны', {
    params: {
        /**
         * Грязный хак, чтобы хранить кейсы в src
         */
        platform: 'Платформа запуска тестов',
    },
    defaultParams: {
        platform: 'touch',
    },
    story: mergeSuites(
        {
            async beforeEach() {
                if (this.params.isAuth) {
                    const profile = profiles['pan-topinambur'];

                    await this.browser.yaLogin(profile.login, profile.password);
                }

                if (this.params.perks) {
                    await this.browser.setState('Loyalty.collections.perks', this.params.perks);
                }
            },
            afterEach() {
                if (this.params.isAuth) {
                    return this.browser.yaLogout();
                }
            },
        },

        prepareSuite(bonusPageBonusCard, {
            params: {
                isAuth: true,
            },
        })
    ),
});
