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
    id: 'm-touch-3643',
    async test() {
        const actionButtonIsVisible = await this.deliveryActionButton.isButtonVisible();

        // Это временный костыль, так как кнопка отображается на декстопе по дефолту, а в таче под экспом,
        // нужно прибраться тут и в pageObject-ах после раскатки/удаления all_checkout_first_flow_removed.
        if (actionButtonIsVisible) {
            await this.deliveryActionButton.click();
        } else {
            await this.deliveryInfo.click();
        }

        await this.browser.yaScenario(this, addressFlow, {
            addressText: addressCardMoscowText,
            addressCheck: addressCardMoscowCheck,
        });

        await this.groupedParcel.changeDeliveryLink.click();

        await this.browser.yaScenario(this, addressFlow, {
            addressText: addressCardSamaraText,
            addressCheck: addressCardSamaraCheck,
        });
    },
});
