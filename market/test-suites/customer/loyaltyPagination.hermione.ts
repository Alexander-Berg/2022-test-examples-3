import {expect} from 'chai';

import {TabsWrapper} from '../../../src/controls/TabsControl/__pageObject__';
import {login, waitForReactRootLoaded} from '../../helpers';
import {Keys} from '../../constants';

const PAGE_URL = '/entity/customer@116848351';

/**
 * Проверяем, что пагинация в таблице лояльности на карточке клиента корректно отображается
 * и отрабатывает при переключении страниц
 */
describe(`ocrm-1727: Пагинация на вкладке "Лояльность" карточки клиента`, () => {
    beforeEach(function() {
        return login(PAGE_URL, this);
    });

    it(`работает корректно`, async function() {
        await waitForReactRootLoaded(this.browser);

        const tabBar = new TabsWrapper(this.browser, 'body', '[data-ow-test-attribute-container="tabsWrapper"]');

        await tabBar.isDisplayed();
        await tabBar.clickTab('Лояльность');
        const table = await (await this.browser.react$('GridTableNew')).getHTML();

        const nextPageButton = await this.browser.$('button[title="Следующая"]');

        await nextPageButton.waitForEnabled();
        await nextPageButton.click();
        const controlsPanel = await nextPageButton.parentElement();
        const pagesInput = await controlsPanel.$('input');

        await pagesInput.isExisting();
        const value2 = await pagesInput.getValue();

        expect(value2).to.equal('2', 'Номер текущей страницы после переключения не соответствует ожидаемому');
        const table2 = await (await this.browser.react$('GridTableNew')).getHTML();

        expect(table2).to.not.equal(table, 'Содержимое таблицы при переключении страниц не изменилось');

        await pagesInput.doubleClick();
        await pagesInput.addValue(`3${Keys.ENTER}`);
        await nextPageButton.waitForEnabled();
        const value3 = await pagesInput.getValue();

        expect(value3).to.equal('3', 'Номер текущей страницы после смены в инпуте не соответствует ожидаемому');
        const table3 = await (await this.browser.react$('GridTableNew')).getHTML();

        expect(table2).to.not.equal(table3, 'Содержимое таблицы при переключении страниц не изменилось');

        const firstPageButton = await this.browser.$('button[title="Первая"]');

        await firstPageButton.waitForEnabled();
        await firstPageButton.click();
        const value1 = await pagesInput.getValue();

        expect(value1).to.equal('1', 'Номер текущей страницы после переключения не соответствует ожидаемому');
        const table1 = await (await this.browser.react$('GridTableNew')).getHTML();

        expect(table1).to.not.equal(table3, 'Содержимое таблицы при переключении страниц не изменилось');
    });
});
