import {makeSuite, prepareSuite} from 'ginny';
// suites
import PreorderSuite from '@self/project/src/spec/hermione/test-suites/blocks/preorder';
// page-objects
import DefaultOffer from '@self/platform/components/DefaultOffer/__pageObject';
import CartButton from '@self/project/src/components/CartButton/__pageObject';

// fixtures
import {ROUTE} from '@self/platform/spec/hermione/fixtures/product/common';
import productWithPreorderDO from '@self/platform/spec/hermione/fixtures/pickup/bluePreorderDO';

export default makeSuite('Синий предзаказ', {
    environment: 'kadavr',
    story: {

        'Дефолтный оффер.': prepareSuite(PreorderSuite, {
            meta: {
                id: 'marketfront-5010',
                issue: 'MARKETFRONT-51879',
            },
            hooks: {
                async beforeEach() {
                    await this.browser.setState(
                        'report',
                        productWithPreorderDO.state
                    );

                    await this.browser.yaOpenPage(
                        'market:product',
                        ROUTE
                    );
                },
            },
            pageObjects: {
                cartButton() {
                    return this.createPageObject(CartButton, {
                        parent: DefaultOffer.root,
                    });
                },
            },
            params: {
                expectedText: 'Оформить предзаказ',
                expectedLink: 'my/checkout',
            },
        }),
    },
});
