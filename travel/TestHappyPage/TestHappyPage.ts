import {assert} from 'chai';

import TestCrossSales from 'helpers/project/happyPage/components/TestCrossSales/TestCrossSales';

import {TestFooter} from 'components/TestFooter';
import TestDesktopOrderHeader from 'components/TestHappyPage/TestDesktopOrderHeader/TestDesktopOrderHeader';
import {Component} from 'components/Component';
import TestSuccessText from 'components/TestHappyPage/TestSuccessText/TestSuccessText';

import {Loader} from '../Loader';
import {Page} from '../Page';
import {TestOrderActions} from '../TestOrderActions';

export class TestHappyPage extends Page {
    desktopOrderHeader: TestDesktopOrderHeader;
    orderActions: TestOrderActions;
    mobileSuccessText: TestSuccessText;
    mobileOrderId: Component;
    crossSales: TestCrossSales;
    loader: Loader;
    footer: TestFooter;

    constructor(browser: WebdriverIO.Browser, qa: QA) {
        super(browser, qa);

        this.desktopOrderHeader = new TestDesktopOrderHeader(this.browser, {
            parent: this.qa,
            current: 'desktopOrderHeader',
        });
        this.orderActions = new TestOrderActions(browser, {
            parent: this.qa,
            current: 'orderActions',
        });
        this.mobileSuccessText = new TestSuccessText(this.browser, {
            parent: this.qa,
            current: 'mobileSuccessText',
        });
        this.mobileOrderId = new Component(this.browser, {
            parent: this.qa,
            current: 'mobileOrderId',
        });
        this.crossSales = new TestCrossSales(browser, {
            parent: this.qa,
            current: 'crossSales',
        });
        this.loader = new Loader(browser);
        this.footer = new TestFooter(browser, 'portalFooter');
    }

    get successText(): TestSuccessText {
        return this.isTouch
            ? this.mobileSuccessText
            : this.desktopOrderHeader.successText;
    }

    get orderId(): Component {
        return this.isTouch
            ? this.mobileOrderId
            : this.desktopOrderHeader.orderId;
    }

    get supportPhone(): Component {
        return this.isTouch
            ? this.footer.supportPhone
            : this.desktopOrderHeader.supportPhone;
    }

    async test(): Promise<void> {
        assert(
            await this.header.isVisible(),
            'На Happy page должен отображаться хедер',
        );
        assert(
            await this.footer.isVisible(),
            'На Happy page должен отображаться футер',
        );
        assert(
            await this.successText.isVisible(),
            'На Happy page должен отображаться заголовок',
        );
        assert(
            await this.orderId.isVisible(),
            'На Happy page должен отображаться номер заказа',
        );
        assert(
            await this.orderActions.detailsLink.isVisible(),
            'На Happy page должна отображаться кнопка "Подробнее о заказе"',
        );
        assert(
            await this.supportPhone.isVisible(),
            'На Happy page должен отображаться телефон поддержки',
        );
    }

    async waitUntilLoaded(): Promise<void> {
        await this.loader.waitUntilLoaded();
    }
}
