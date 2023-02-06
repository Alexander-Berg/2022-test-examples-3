import {makeSuite, mergeSuites, prepareSuite} from '@yandex-market/ginny';

import ExpressDelivery from '@self/root/src/widgets/parts/SinsHeader/components/ExpressDelivery/__pageObject/index.touch';
import Rating from '@self/root/src/widgets/parts/SinsHeader/components/Rating/__pageObject/index.touch';
import Schedule from '@self/root/src/widgets/parts/SinsHeader/components/Schedule/__pageObject/index.touch';
import BottomDrawerPopup from '@self/root/src/components/BottomDrawerPopup/__pageObject';

import {PAGE_IDS_TOUCH} from '@self/root/src/constants/pageIds/commonPageIds';
import SinsHeaderSuite from '@self/platform/spec/hermione2/test-suites/blocks/SinsHeader';

import businessTreeMock from './fixtures/businessTree';
import {createState} from './fixtures/reportSearchWithExpress';

// eslint-disable-next-line import/no-commonjs
module.exports = makeSuite('SinS', {
    environment: 'kadavr',
    feature: 'SinS',
    story: makeSuite('Express страница магазина', {
        story: mergeSuites(
            {
                async beforeEach() {
                    hermione.setPageObjects.call(this, {
                        expressDelivery: () => this.browser.createPageObject(ExpressDelivery),
                        rating: () => this.browser.createPageObject(Rating),
                        schedule: () => this.browser.createPageObject(Schedule),
                        bottomDrawerPopup: () => this.browser.createPageObject(BottomDrawerPopup),
                    });

                    await this.browser.setState('Cataloger.businessTree', businessTreeMock);
                    await this.browser.setState('report', createState());

                    return this.browser.yaOpenPage(PAGE_IDS_TOUCH.BUSINESS, {
                        slug: 'slug',
                        businessId: '10671581',
                        express: 'express',
                    });
                },
            },
            prepareSuite(SinsHeaderSuite, {
                params: {
                    expectedTitleLogoLink: '/business--slug/10671581',
                },
            })
        ),
    }),
});
