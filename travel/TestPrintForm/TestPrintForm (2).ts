import {SECOND} from 'helpers/constants/dates';

import {Component} from 'components/Component';

/**
 * Форма печати из браузера
 * disclaimer: остальные методы компонента надо реализовывать по необходимости
 */
export class TestPrintForm extends Component {
    /**
     * #id - не работает поиск по id в firefox
     */
    private frameSelector: string = '[id=printJS]';

    async isOpened(timeout: number = 30 * SECOND): Promise<boolean> {
        await this.browser.waitForExist(this.frameSelector, timeout);

        return this.browser.isExisting(this.frameSelector);
    }
}
