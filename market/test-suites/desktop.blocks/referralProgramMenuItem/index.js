import {makeSuite, mergeSuites} from 'ginny';

import commonSuites from '@self/root/src/spec/hermione/test-suites/blocks/referralProgramMenuItem';

export default makeSuite('Пункт меню "Приглашайте друзей".', {
    environment: 'kadavr',
    issue: 'MARKETFRONT-50643',
    feature: 'Реферальная программа',
    defaultParams: {
        isAuthWithPlugin: true,
    },
    story: mergeSuites(commonSuites),
});
