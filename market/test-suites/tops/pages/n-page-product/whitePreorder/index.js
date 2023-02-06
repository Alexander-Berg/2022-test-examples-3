import {makeSuite, prepareSuite} from 'ginny';
// suites
import PreOrderTextSuite from '@self/platform/spec/hermione/test-suites/blocks/n-delivery/preOrderText';
// page-objects
import Delivery from '@self/platform/spec/page-objects/components/DeliveryInfo';
import DefaultOffer from '@self/platform/components/DefaultOffer/__pageObject';
// fixtures
import {ROUTE} from '@self/platform/spec/hermione/fixtures/product/common';
import preOrderOnlyDO from '@self/platform/spec/hermione/fixtures/pickup/preOrderOnlyDO';

export default makeSuite('Белый предзаказ', {
    environment: 'kadavr',
    story: {

        'Дефолтный оффер.': prepareSuite(PreOrderTextSuite, {
            meta: {
                id: 'marketfront-4056',
                issue: 'MARKETFRONT-10965',
            },
            hooks: {
                async beforeEach() {
                    await this.browser.setState(
                        'report',
                        preOrderOnlyDO.state
                    );

                    await this.browser.yaOpenPage(
                        'market:product',
                        ROUTE
                    );
                },
            },
            pageObjects: {
                delivery() {
                    return this.createPageObject(Delivery, {
                        parent: DefaultOffer.root,
                    });
                },
            },
            params: {
                expectedText: 'Предзаказ',
            },
        }),
    },
});
