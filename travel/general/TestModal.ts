import {Component} from './Component';
import {Button} from './Button';

export class TestModal extends Component {
    readonly closeButton: Button;

    constructor(browser: WebdriverIO.Browser, qa: QA = 'modal') {
        super(browser, qa);

        this.closeButton = new Button(browser, {
            parent: this.qa,
            current: 'closeButton',
        });
    }

    /** @deprecated - метод смотрит на косвенный технический признак отображения модала. Такой способ не надежен. Надо смотреть на реальное отображение */
    async isOpened(): Promise<boolean> {
        const visible = await this.getAttribute('data-visible');

        return visible === 'true';
    }

    /**
     * Переопределение метода из Components, т.к. isDisplayed для модала вседа возвращает true,
     * т.к. для элементов скрытых за вьюпортом, подробнее тут: https://webdriver.io/docs/api/element/isDisplayed/
     */
    async waitForVisible(timeout?: number): Promise<void> {
        await this.waitUntil(
            async () => await this.isDisplayedInViewport(),
            timeout,
        );
    }

    /**
     * Переопределение метода из Components, т.к. isDisplayed для модала вседа возвращает true,
     * т.к. для элементов скрытых за вьюпортом, подробнее тут: https://webdriver.io/docs/api/element/isDisplayed/
     */
    async waitForHidden(timeout?: number): Promise<void> {
        await this.waitUntil(
            async () => !(await this.isDisplayedInViewport()),
            timeout,
            "Modal didn't close",
        );
    }
}
