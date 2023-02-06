import {makeSuite, prepareSuite, mergeSuites} from 'ginny';

import LogoSuite from '@self/platform/spec/hermione/test-suites/blocks/header2/__logo';
/**
 * Тесты на блок header2.
 * @param {PageObject.Header2} header2
 */
export default makeSuite('Блок header2.', {
    feature: 'Хедер',
    story: mergeSuites(
        prepareSuite(LogoSuite)
    ),
});
