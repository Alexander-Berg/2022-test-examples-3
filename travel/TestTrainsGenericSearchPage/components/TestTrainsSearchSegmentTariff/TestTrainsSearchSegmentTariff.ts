import _get from 'lodash/get';

import {Component} from 'components/Component';
import {TestPrice} from 'components/TestPrice';

export enum ETrainsSegmentTariffTitle {
    PLATZKARTE = 'плац',
    COMPARTMENT = 'купе',
    SUITE = 'св',
    COMMON = 'общ',
    SITTING = 'сид',
    SOFT = 'люкс',
}

export class TestTrainsSearchSegmentTariff extends Component {
    title: Component;
    seats: Component;
    price: TestPrice;

    constructor(browser: WebdriverIO.Browser, qa: QA) {
        super(browser, qa);

        this.title = new Component(browser, {
            parent: this.qa,
            current: 'title',
        });
        this.seats = new Component(browser, {
            parent: this.qa,
            current: 'seats',
        });
        this.price = new TestPrice(browser, {
            parent: this.qa,
            current: 'price',
        });
    }

    checkTariffFieldDisplay = async (fieldName: string): Promise<void> => {
        const isDisplayed = await _get(this, fieldName).isDisplayed();

        if (!isDisplayed) {
            throw new Error(
                `В тарифе должно быть отображено поле: ${fieldName}`,
            );
        }
    };

    checkTariffFields = async (): Promise<void> => {
        const tariffNeedCheckFields = ['title', 'seats', 'price'];
        const textPrice = await this.price.getText();

        if (!textPrice.includes('от')) {
            throw new Error('В тарифе цена должна содержать приписку "от"');
        }

        for (const fieldName of tariffNeedCheckFields) {
            try {
                await this.checkTariffFieldDisplay(fieldName);
            } catch {
                throw new Error(
                    `В тарифе должно быть отображено поле: ${fieldName}`,
                );
            }
        }
    };

    isTariffMatchesToTitle = async (
        tariffTitle: ETrainsSegmentTariffTitle,
    ): Promise<boolean> => {
        const title = await this.title.getText();
        const tariffTitleReqExp = new RegExp(tariffTitle, 'i');

        return tariffTitleReqExp.test(title);
    };
}
