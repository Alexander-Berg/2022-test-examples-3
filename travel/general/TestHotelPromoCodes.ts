import {Component, ComponentArray} from 'helpers/project/common/components';
import {TestPrice} from 'helpers/project/common/components/TestPrice';

type TTestHotelPromoCode = {
    promoCode: Component;
    promoCodeDiscount: TestPrice;
};

export default class TestHotelPromoCodes {
    private promoCodes: ComponentArray<Component>;
    private promoCodeDiscounts: ComponentArray<TestPrice>;

    constructor(browser: WebdriverIO.Browser) {
        this.promoCodes = new ComponentArray(browser, 'promoCode', Component);
        this.promoCodeDiscounts = new ComponentArray(
            browser,
            'promoCodeDiscount',
            TestPrice,
        );
    }

    async getItems(): Promise<TTestHotelPromoCode[]> {
        const promoCodes = await this.promoCodes.items;
        const promoCodeDiscounts = await this.promoCodeDiscounts.items;

        return promoCodes.map((promoCode, index) => ({
            promoCode,
            promoCodeDiscount: promoCodeDiscounts[index],
        }));
    }
}
