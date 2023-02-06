import {makeCase} from 'ginny';

import {
    addressFlow,
    addressCardMoscowText,
    addressCardSamaraText,
    addressCardMoscowCheck,
    addressCardSamaraCheck,
} from './checks';

export default makeCase({
    issue: 'MARKETFRONT-50730',
    id: 'marketfront-4893',
    async test() {
        await this.deliveryActionButton.isButtonVisible();
        await this.deliveryActionButton.click();

        await this.browser.yaScenario(this, addressFlow, {
            addressText: addressCardMoscowText,
            addressCheck: addressCardMoscowCheck,
        });

        await this.editableCard.changeButtonClick();

        await this.browser.yaScenario(this, addressFlow, {
            addressText: addressCardSamaraText,
            addressCheck: addressCardSamaraCheck,
        });
    },
});
