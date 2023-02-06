import {makeCase} from 'ginny';

import {openCartPage}
    from '@self/root/src/spec/hermione/scenarios/cart';
import {waitCallingUserLastState, waitPreloader}
    from '@self/root/src/spec/hermione/scenarios/checkout';
import {goToConfirmationPage} from '@self/root/src/spec/hermione/scenarios/checkout/goToConfirmationPage';
import userFormData from '@self/root/src/spec/hermione/configs/checkout/formData/user-prepaid';

import {moscowAddress} from '../mocks';
import {
    addressCardCheck,
    dateCheck,
    parcelCardTitleCheck,
    timeCheck,
    recipientCardCheck,
    paymentCardCheck,
} from './checks';

export default makeCase({
    async test() {
        await this.cartCheckoutButton.waitForButtonEnabled();
        await this.cartCheckoutButton.goToCheckout();

        if (!this.params.isAuthWithPlugin) {
            await this.agitationModal.waitForVisible();
            await this.browser.yaWaitForChangeUrl(() => this.notNowButton.click());
        }

        await this.browser.yaScenario(this, goToConfirmationPage, {
            userFormData,
            addressFormData: moscowAddress,
        });

        await this.browser.yaScenario(this, addressCardCheck);

        await this.dateSelect.click();
        await this.selectPopover.waitForListIsVisible();
        await this.selectPopover.clickOnLastOption();
        await this.browser.yaScenario(this, dateCheck);

        await this.browser.yaScenario(this, parcelCardTitleCheck);

        await this.timeSelect.clickOnLastOption();

        await this.browser.yaScenario(this, timeCheck);

        await this.browser.yaScenario(this, recipientCardCheck);

        await this.editPaymentOptionCard.changeButtonClick();

        await this.paymentOptionsList.setPaymentTypeCashOnDelivery();
        await this.browser.yaScenario(
            this,
            waitCallingUserLastState,
            () => this.paymentOptionsList.submitButtonClick()
        );

        await this.browser.yaScenario(this, paymentCardCheck);

        await this.browser.yaScenario(this, waitPreloader);
        await this.browser.yaScenario(this, openCartPage);

        await this.cartCheckoutButton.waitForButtonEnabled();
        await this.cartCheckoutButton.goToCheckout();

        if (!this.params.isAuthWithPlugin) {
            await this.agitationModal.waitForVisible();
            await this.browser.yaWaitForChangeUrl(() => this.notNowButton.click());
        }

        await this.browser.yaScenario(this, waitPreloader);

        await this.browser.yaScenario(this, addressCardCheck);
        await this.browser.yaScenario(this, dateCheck);
        await this.browser.yaScenario(this, parcelCardTitleCheck);
        await this.browser.yaScenario(this, timeCheck);
        await this.browser.yaScenario(this, recipientCardCheck);
        await this.browser.yaScenario(this, paymentCardCheck);
    },
});
