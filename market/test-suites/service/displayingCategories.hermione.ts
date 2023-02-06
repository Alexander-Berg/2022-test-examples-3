/* eslint-disable no-await-in-loop */

import 'hermione';
import {expect} from 'chai';

import {TabsWrapper} from '../../../src/controls/TabsControl/__pageObject__';
import {login, pause} from '../../helpers';
import Button from '../../page-objects/button';
import ContentWithLabel from '../../page-objects/contentWithLabel';
import {CHECK_INTERVAL, CLEAR_ALL_SEQUENCE, CLEAR_SEQUENCE, Keys, LONG_TIMEOUT_MS} from '../../constants';

const LINE_URL = '/entity/service@152431084';
const TICKET_URL = '/entity/ticket@213188085/edit';
const BRAND_URL = '/entity/brand@139806585';
const BANNER_CATEGORY_TITLE = 'Как разместить баннер';
const FEEDBACK_CATEGORY_TITLE = 'Отзывы и рейтинг';
const TICKET_DEFAULT_CATEGORIES_URLS = {
    [BANNER_CATEGORY_TITLE]: '/entity/ticketCategory@152208741',
    [FEEDBACK_CATEGORY_TITLE]: '/entity/ticketCategory@152208501',
};

/**
 * Проверяем, что:
 * в очереди "Маркет: Аккаунт" стоит галка "Использовать очередь для фильтрации списка категорий";
 * у доступных для выбора категорий обращения тикета в очереди "Маркет: Аккаунт"
 * в свойствах указана очередь "Маркет: Аккаунт".
 */
describe('ocrm-1317: Отображение категорий, привязанных к очереди', () => {
    beforeEach(function() {
        return login(LINE_URL, this);
    });

    it('Проверяет, что все отображаемые для выбора в тикете категории относятся к очереди "Маркет: Аккаунт"', async function() {
        const tabBar = new TabsWrapper(this.browser, 'body', '[data-ow-test-attribute-container="tabsWrapper"]');
        const editButton = new Button(
            this.browser,
            'body',
            '[data-ow-test-jmf-card-toolbar-action="goToEdit-изменить"]'
        );

        await tabBar.isDisplayed();
        await tabBar.clickTab('Свойства');
        const usedToFilterCategories = this.browser
            .$('[data-ow-test-attribute-container="usedToFilterCategories"] [data-ow-test-checkbox="true"]')
            .isExisting();

        if (!usedToFilterCategories) {
            await editButton.clickButton();
            const usedToFilterCategoriesCheckbox = this.browser.$(
                '[data-ow-test-attribute-container="usedToFilterCategories"] [data-ow-test-checkbox]'
            );

            await usedToFilterCategoriesCheckbox.isEnabled();
            await usedToFilterCategoriesCheckbox.click();
            const usedToFilterCategoriesAfterClick = this.browser
                .$('[data-ow-test-attribute-container="usedToFilterCategories"] [data-ow-test-checkbox="true"]')
                .isExisting();

            expect(usedToFilterCategoriesAfterClick).to.equal(
                true,
                "В очереди не стоит галка 'Использовать очередь для фильтрации списка категорий'"
            );
        }

        await this.browser.url(TICKET_URL);

        const categoriesInput = new ContentWithLabel(
            this.browser,
            'body',
            '[data-ow-test-attribute-container="categories"]'
        );

        await categoriesInput.isDisplayed();
        await categoriesInput.click();
        const popupCategories = await this.browser.$('[data-ow-test-popup]');

        await popupCategories.isDisplayed();
        const expandButton = await popupCategories.$('button svg');

        await expandButton.waitForExist();
        const expandButtons = await popupCategories.$$('button svg');

        for (const button of expandButtons) {
            await button.click();
        }

        await pause(200);

        const categories = await popupCategories.$$('[data-ow-test-checkbox] ~ div');
        const categoriesTitles = await Promise.all(categories.map(async category => category.$('span').getText()));

        expect(categoriesTitles.length).to.not.equal(0, 'Нет доступных для выбора категорий');

        await this.browser.url(BRAND_URL);
        const categoriesBlock = new ContentWithLabel(
            this.browser,
            'body',
            '[data-ow-test-content="table-ticketCategory-forList"]'
        );

        await categoriesBlock.isDisplayed();
        const searchInput = this.browser.$('[data-ow-test-table-toolbar="search-input"] input');

        await searchInput.isDisplayed();

        const categoriesUrls: string[] = [];

        for (const title of categoriesTitles) {
            await searchInput.doubleClick();
            await searchInput.addValue(CLEAR_SEQUENCE);
            await searchInput.setValue(Array(25).fill(CLEAR_SEQUENCE));
            await searchInput.addValue(CLEAR_ALL_SEQUENCE);
            await searchInput.addValue(title + Keys.ENTER);

            await pause(300);

            const tableValueTitle = await this.browser.$('[data-ow-test-table-row-0="title"]');

            const noDataText = await this.browser.$('//span[text()="Нет данных"]');

            await this.browser.waitUntil(
                // eslint-disable-next-line no-return-await
                async () => (await tableValueTitle.isDisplayed()) || (await noDataText.isExisting()),
                {
                    timeout: LONG_TIMEOUT_MS,
                    interval: CHECK_INTERVAL,
                    timeoutMsg: 'Не дождались результата поиска',
                }
            );

            const titleIsDisplayed = await tableValueTitle.isExisting();

            if (titleIsDisplayed) {
                const href = await this.browser
                    .$('[data-ow-test-table-row-0="title"]')
                    .$('a')
                    .getAttribute('href');

                categoriesUrls.push(href.toString());
            } else if (title === BANNER_CATEGORY_TITLE || title === FEEDBACK_CATEGORY_TITLE) {
                categoriesUrls.push(TICKET_DEFAULT_CATEGORIES_URLS[title]);
            } else {
                expect(titleIsDisplayed).to.equal(true, 'В таблице категорий не нашли категорию из тикета');
            }

            await searchInput.doubleClick();
            await searchInput.addValue(CLEAR_SEQUENCE);
            await searchInput.addValue(CLEAR_ALL_SEQUENCE);
            await searchInput.setValue(Array(25).fill(CLEAR_SEQUENCE));
        }

        const properties = new ContentWithLabel(this.browser, 'body', '[data-ow-test-content="properties-mainAttrs"]');

        for (const href of categoriesUrls) {
            await this.browser.url(href);
            await properties.isExisting();
            const services = await this.browser.$('[data-ow-test-attribute-container="services"]');

            await services.isExisting();
            const serviceExist = await services.$('//a[text()="Маркет: Аккаунт"]').isExisting();

            expect(serviceExist).to.equal(true, 'У отображаемой категории нет нужной очереди');
        }
    });
});
