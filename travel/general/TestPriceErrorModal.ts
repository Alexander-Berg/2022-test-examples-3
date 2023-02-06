import {assert} from 'chai';

import {TestPrice} from 'components/TestPrice';
import {TestErrorModal} from 'components/TestErrorModal';

export interface ITestPriceErrorModalContent {
    title: string;
    text: string;
    secondaryActionText: string;
    primaryActionText: string;
    priceValue: number;
}

export class TestPriceErrorModal extends TestErrorModal {
    readonly price: TestPrice;
    readonly lastPrice: TestPrice;

    constructor(browser: WebdriverIO.Browser, qa: QA) {
        super(browser, qa);

        this.price = new TestPrice(browser, {
            parent: this.qa,
            current: 'price',
        });
        this.lastPrice = new TestPrice(browser, {
            parent: this.qa,
            current: 'lastPrice',
        });
    }

    async testModalContent(
        options: ITestPriceErrorModalContent,
    ): Promise<void> {
        const {
            title,
            text,
            secondaryActionText,
            primaryActionText,
            priceValue,
        } = options;

        assert(await this.isVisible(), 'Должен появиться модал ошибки с ценой');
        assert.equal(
            await this.title.getText(),
            title,
            `Текст заголовока в модале ошибки оплаты должен соотвествовать: "${title}"`,
        );
        assert.equal(
            await this.text.getText(),
            text,
            `Текст в модале ошибки оплаты должен соотвествовать: "${text}"`,
        );
        assert.equal(
            await this.secondaryActionButton.getText(),
            secondaryActionText,
            `Текст кнопки второстепенного действия в модале ошибки оплаты должен соотвествовать: "${secondaryActionText}"`,
        );
        assert.equal(
            await this.primaryActionButton.getText(),
            primaryActionText,
            `Текст кнопки основного действия в модале ошибки оплаты должен соотвествовать: "${primaryActionText}"`,
        );
        assert.equal(
            await this.price.getPriceValue(),
            priceValue,
            `Сумма заказа в модале ошибки должна соотвествовать ${priceValue}`,
        );
    }
}
