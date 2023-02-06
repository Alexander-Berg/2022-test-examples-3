import {prepareSuite} from 'ginny';
import {createOffer} from '@yandex-market/kadavr/mocks/Report/helpers';

import DrugsDisclaimerSuite from '@self/platform/spec/hermione/test-suites/blocks/DrugsDisclaimer';
import DrugsDisclaimer from '@self/platform/widgets/parts/DrugsDisclaimer/__pageObject';
import RegionPopup from '@self/platform/spec/page-objects/widgets/parts/RegionPopup';

export default prepareSuite(DrugsDisclaimerSuite, {
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
            const OFFER_ID = 'OiA--W5SL07NrstdAn5kXg';

            const offer = createOffer({
                urls: {
                    encrypted: '/redir/',
                },
                shop: {
                    id: 774,
                    slug: 'shop',
                    name: 'shop',
                },
            }, OFFER_ID);

            await this.browser.setState('report', offer);
            await this.browser.yaOpenPage('touch:offer', {offerId: OFFER_ID});
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
});
