import {prepareSuite, makeSuite} from 'ginny';

import {productWithDefaultOffer, phoneProductRoute} from '@self/platform/spec/hermione/fixtures/product';
import DrugsDisclaimerSuite from '@self/platform/spec/hermione/test-suites/blocks/DrugsDisclaimer';
import DrugsDisclaimer from '@self/platform/widgets/parts/DrugsDisclaimer/__pageObject';
import RegionPopup from '@self/platform/spec/page-objects/widgets/parts/RegionPopup';

export default makeSuite('Лекарственный дисклеймер', {
    environment: 'kadavr',
    story: prepareSuite(DrugsDisclaimerSuite, {
        meta: {
            id: 'm-touch-3243',
            issue: 'MARKETFRONT-7776',
        },
        pageObjects: {
            drugsDisclaimer() {
                return this.createPageObject(DrugsDisclaimer);
            },
        },
        hooks: {
            async beforeEach() {
                await this.browser.setState('report', productWithDefaultOffer);
                await this.browser.yaOpenPage('touch:product', phoneProductRoute);
                await this.browser.yaClosePopup(this.createPageObject(RegionPopup));

                await this.browser.allure.runStep(
                    'Ждём загрузки блока c юридическим дисклеймером о покупке лекарств',
                    () => this.drugsDisclaimer.waitForVisible()
                );

                await this.browser.allure.runStep(
                    'Скроллим до блока c юридическим дисклеймером о покупке в кредит',
                    () => this.drugsDisclaimer
                        .getSelector()
                        .then(selector => this.browser.scroll(selector, 0, -200))
                );
            },
        },
    }),
});
