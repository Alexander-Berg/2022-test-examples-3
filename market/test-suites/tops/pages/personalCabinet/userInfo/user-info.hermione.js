import {prepareSuite, mergeSuites, makeSuite} from 'ginny';

import {createUser} from '@yandex-market/kadavr/mocks/PersQa/helpers';

// configs
import {profiles} from '@self/platform/spec/hermione/configs/profiles';
// suites
import ProfileInfoSuite from '@self/platform/spec/hermione/test-suites/blocks/components/ProfileInfo';

const USER_PROFILE_CONFIG = profiles.ugctest3;
const DEFAULT_USER = createUser({
    id: USER_PROFILE_CONFIG.uid,
    uid: {
        value: USER_PROFILE_CONFIG.uid,
    },
    login: USER_PROFILE_CONFIG.login,
    display_name: {
        name: 'Willy Wonka',
        public_name: 'Willy W.',
        avatar: {
            default: '61207/462703116-1544492602',
            empty: false,
        },
    },
    dbfields: {
        'userinfo.firstname.uid': 'Willy',
        'userinfo.lastname.uid': 'Wonka',
    },
});

// eslint-disable-next-line import/no-commonjs
module.exports = makeSuite('Страница личного кабинета.', {
    environment: 'kadavr',
    story: mergeSuites(
        {
            async beforeEach() {
                await this.browser.setState('schema', {
                    users: [DEFAULT_USER],
                });
                await this.browser.yaProfile(USER_PROFILE_CONFIG.login, 'touch:index');
                await this.browser.yaOpenPage('touch:my-tasks');
            },
        },
        prepareSuite(ProfileInfoSuite)
    ),
});
