import {assert} from 'chai';

import {Component} from './Component';
import {Loader} from './Loader';
import {Page} from './Page';
import {TestOrderActions} from './TestOrderActions';
import {TestOrderHeader} from './TestOrderHeader';

export class TestHappyPage extends Page {
    orderHeader: TestOrderHeader;
    orderActions: TestOrderActions;
    supportPhone: Component;
    loader: Loader;

    constructor(browser: WebdriverIO.Browser, qa: QA) {
        super(browser, qa);

        this.orderHeader = new TestOrderHeader(browser);
        this.orderActions = new TestOrderActions(browser);
        this.supportPhone = new Component(browser, 'support-phone');
        this.loader = new Loader(browser);
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
            await this.orderHeader.successBadge.isVisible(),
            'На Happy page должен отображаться заголовок',
        );
        assert(
            await this.orderHeader.numberBlock.isVisible(),
            'На Happy page должен отображаться номер заказа',
        );
        assert(
            await this.orderActions.detailsLink.isVisible(),
            'На Happy page должна отображаться кнопка "Подробнее о заказе" ',
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
