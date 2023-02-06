import {assert} from 'chai';

import {SECOND} from 'helpers/constants/dates';

import {Button, Input} from 'helpers/project/common/components';
import {TestCheckbox} from 'helpers/project/common/components/TestCheckbox';

import {Component} from 'components/Component';
import {TestPrice} from 'components/TestPrice';

export class TestPromoCodes extends Component {
    readonly checkBox: TestCheckbox;
    readonly input: Input;
    readonly button: Button;
    readonly authText: Component;
    readonly tooltip: Component;
    readonly discountPrice: TestPrice;
    readonly resetLinkButton: Button;

    constructor(browser: WebdriverIO.Browser, qa?: QA) {
        super(browser, qa);

        this.checkBox = new TestCheckbox(browser, {
            parent: this.qa,
            current: 'checkbox',
        });
        this.input = new Input(browser, {
            parent: this.qa,
            current: 'input',
        });
        this.button = new Button(browser, {
            parent: this.qa,
            current: 'button',
        });
        this.authText = new Component(browser, {
            parent: this.qa,
            current: 'authText',
        });
        this.tooltip = new Component(browser, {
            parent: this.qa,
            current: 'tooltip',
        });
        this.discountPrice = new TestPrice(browser, {
            parent: this.qa,
            current: 'discountAmount',
        });
        this.resetLinkButton = new Button(browser, {
            parent: this.qa,
            current: 'reset',
        });
    }

    async applyPromoCode(
        promoCode: string,
        waitMsForPrice: number | null = 25 * SECOND,
    ): Promise<void> {
        await this.input.type(promoCode);
        await this.button.click();

        if (waitMsForPrice) {
            await this.discountPrice.waitForVisible(waitMsForPrice);
        }
    }

    /* Test helpers */

    async testInitialPromoCodesState(): Promise<void> {
        assert.isFalse(
            await this.checkBox.isChecked(),
            'Чекбокс промокода должен быть выключен по умолчанию',
        );
        assert.isFalse(
            await this.input.isVisible(),
            'Поле ввода промокода должно быть спрятано до нажатия на чекбокс',
        );
        assert.isFalse(
            await this.button.isVisible(),
            'Кнопка примения промокода должна быть спрятаня до нажатия на чекбокс',
        );
    }

    async testActivePromoCodesState(): Promise<void> {
        assert(
            await this.input.isVisible(),
            'Поле ввода промокода должно отображаться после нажатия на чекбокс',
        );
        assert(
            await this.button.isVisible(),
            'Кнопка примения промокода должна отображаться после нажатия на чекбокс',
        );
        assert(
            await this.button.isDisabled(),
            'Кнопка примения промокода должна заблокирована при пустом поле ввода промокода',
        );
    }

    async testTooltipError(value: string, error: string): Promise<void> {
        await this.applyPromoCode(value, null);
        await this.tooltip.waitForVisible(5000);

        const errorText = await this.tooltip.getText();

        assert.equal(
            errorText,
            error,
            `Текст ошибки применения промокода должен соотвествовать: "${error}"`,
        );
    }
}
