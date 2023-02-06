import {prepareSuite, makeSuite} from 'ginny';

import {productWithDefaultOffer, phoneProductRoute} from '@self/platform/spec/hermione/fixtures/product';
import LinkSellersInfoNotInStockSuite
    from '@self/platform/spec/hermione/test-suites/blocks/CreditDisclaimer/linkSellersInfoNotInStock';
import CreditDisclaimer from '@self/platform/widgets/parts/CreditDisclaimer/__pageObject';
import RegionPopup from '@self/platform/spec/page-objects/widgets/parts/RegionPopup';

export default makeSuite('Ссылка "Информация о продавцах".', {
    environment: 'kadavr',
    story:
        prepareSuite(LinkSellersInfoNotInStockSuite, {
            pageObjects: {
                creditDisclaimer() {
                    return this.createPageObject(CreditDisclaimer);
                },
            },
            hooks: {
                async beforeEach() {
                    await this.browser.setState('report', productWithDefaultOffer);

                    await this.browser.yaOpenPage('touch:product', phoneProductRoute);
                    await this.browser.yaClosePopup(this.createPageObject(RegionPopup));

                    await this.browser.allure.runStep(
                        'Ждём загрузки блока c юридическим дисклеймером о покупке в кредит',
                        () => this.creditDisclaimer.waitForVisible()
                    );
                },
            },
        }),
});
