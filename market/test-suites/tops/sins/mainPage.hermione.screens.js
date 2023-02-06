import {makeSuite, mergeSuites, prepareSuite} from '@yandex-market/ginny';

import SinsHeaderSuite from '@self/platform/spec/hermione2/test-suites/blocks/SinsHeader/index.screens';
import {
    HeaderSearchScreensSuite,
} from '@self/platform/spec/hermione2/test-suites/blocks/HeaderSearch';

import businessTreeMock from './fixtures/businessTree';
import {createState} from './fixtures/reportSearch';

// eslint-disable-next-line import/no-commonjs
module.exports = makeSuite('SinS', {
    environment: 'kadavr',
    feature: 'SinS',
    story: makeSuite('Главная страница магазина', {
        story: mergeSuites(
            {
                async beforeEach() {
                    await this.browser.setState('Cataloger.businessTree', businessTreeMock);
                    await this.browser.setState('report', createState());

                    return this.browser.yaOpenPage('market:business', {
                        slug: 'slug',
                        businessId: '10671581',
                    });
                },
            },
            prepareSuite(SinsHeaderSuite),
            prepareSuite(HeaderSearchScreensSuite)
        ),
    }),
});
