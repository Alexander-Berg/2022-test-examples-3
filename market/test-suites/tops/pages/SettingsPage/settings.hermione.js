import {mergeSuites, prepareSuite, makeSuite} from 'ginny';

// suites
import Header2Suite from '@self/platform/spec/hermione/test-suites/blocks/header2';
// page-objects
import Header2 from '@self/platform/spec/page-objects/header2';

import banners from './banners';

// eslint-disable-next-line import/no-commonjs
module.exports = makeSuite('Страница настроек.', {
    issue: 'MARKETVERSTKA-25711',
    environment: 'testing',
    story: mergeSuites(
        makeSuite('Хедер.', {
            story: prepareSuite(Header2Suite, {
                pageObjects: {
                    header2() {
                        return this.createPageObject(Header2);
                    },
                },
                hooks: {
                    beforeEach() {
                        return this.browser.yaOpenPage('market:my-settings');
                    },
                },
            }),
        }),
        banners
    ),
});
