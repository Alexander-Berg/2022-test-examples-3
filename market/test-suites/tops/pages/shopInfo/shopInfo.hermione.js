import {prepareSuite, makeSuite} from 'ginny';

import {createStories} from '@self/platform/spec/hermione/helpers/createStories';
import seoTestConfigs from '@self/platform/spec/hermione/configs/seo/shop-info-page';
import BaseLinkCanonicalSuite from '@self/platform/spec/hermione/test-suites/blocks/n-base/__link-canonical';
import Base from '@self/platform/spec/page-objects/n-base';

// eslint-disable-next-line import/no-commonjs
module.exports = makeSuite('Страница информации магазина.', {
    environment: 'testing',
    story: {
        'SEO-разметка страницы.': createStories(
            seoTestConfigs,
            ({routeConfig, testParams}) => prepareSuite(BaseLinkCanonicalSuite, {
                hooks: {
                    beforeEach() {
                        return this.browser
                            .yaSimulateBot()
                            .yaOpenPage('touch:shop-info', routeConfig);
                    },
                },
                pageObjects: {
                    base() {
                        return this.createPageObject(Base);
                    },
                },
                params: testParams,
            })
        ),
    },
});
