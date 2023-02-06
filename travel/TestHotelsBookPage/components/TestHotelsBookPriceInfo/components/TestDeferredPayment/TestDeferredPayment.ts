import {assert} from 'chai';

import {TestCheckbox} from 'helpers/project/common/components/TestCheckbox';

import {Component} from 'components/Component';
import {TestPrice} from 'components/TestPrice';

export class TestDeferredPayment extends Component {
    readonly deferredPaymentCheckbox: TestCheckbox;

    readonly fullPaymentCheckbox: TestCheckbox;

    readonly paymentEndsAt: Component;

    readonly postpayRadioboxLabel: Component;

    readonly prepayPrice: TestPrice;

    readonly postpayPrice: TestPrice;

    readonly fullPrice: TestPrice;

    constructor(browser: WebdriverIO.Browser, qa?: QA) {
        super(browser, qa);

        this.deferredPaymentCheckbox = new TestCheckbox(browser, {
            parent: this.qa,
            current: 'deferredPaymentCheckbox',
        });

        this.fullPaymentCheckbox = new TestCheckbox(browser, {
            parent: this.qa,
            current: 'fullPaymentCheckbox',
        });

        this.prepayPrice = new TestPrice(browser, {
            parent: this.qa,
            current: 'prepayPrice',
        });

        this.postpayPrice = new TestPrice(browser, {
            parent: this.qa,
            current: 'postpayPrice',
        });

        this.postpayRadioboxLabel = new Component(browser, {
            parent: this.qa,
            current: 'postpayRadioboxLabel',
        });

        this.fullPrice = new TestPrice(browser, {
            parent: this.qa,
            current: 'fullPrice',
        });

        this.paymentEndsAt = new Component(browser, {
            parent: this.qa,
            current: 'paymentEndsAt',
        });
    }

    async applyDeferredPayment(): Promise<void> {
        await this.deferredPaymentCheckbox.scrollIntoView();
        await this.deferredPaymentCheckbox.click();
    }

    async applyFullPayment(): Promise<void> {
        await this.fullPaymentCheckbox.scrollIntoView();
        await this.fullPaymentCheckbox.click();
    }

    async tryApplyFullPayment(): Promise<void> {
        if (await this.fullPaymentCheckbox.isVisible()) {
            await this.applyFullPayment();
        }
    }

    async getPrepayPrice(): Promise<number> {
        return await this.prepayPrice.getValue();
    }

    async getPostpayPrice(): Promise<number> {
        return await this.postpayPrice.getValue();
    }

    async getFullPrice(): Promise<number> {
        return await this.fullPrice.getValue();
    }

    async checkActiveDeferredPaymentCheckbox(): Promise<boolean> {
        const dataActiveValue = await this.deferredPaymentCheckbox.getAttribute(
            'data-active',
        );

        return dataActiveValue === 'true';
    }

    async testAvailabilityAllCheckbox(): Promise<void> {
        assert.isTrue(
            (await this.deferredPaymentCheckbox.isVisible()) &&
                (await this.fullPaymentCheckbox.isVisible()),
            'Чекбоксы "Оплатить позже" и "полная оплата" должны отображаться',
        );
    }

    async testHasPaymentEndsAtLabel(): Promise<void> {
        assert.isTrue(
            await this.paymentEndsAt.isVisible(),
            'Крайняя дата оплаты должна отображаться',
        );
    }
}
