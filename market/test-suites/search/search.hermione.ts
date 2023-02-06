import 'hermione';
import {expect} from 'chai';

import {SearchBlockWithText, SearchInput} from '../../../src/pages/SearchPage/__pageObject__';
import Button from '../../page-objects/button';
import {login, createOutgoingTicket} from '../../helpers';
import {CHECK_INTERVAL, LONG_TIMEOUT_MS} from '../../constants';
import ContentWithLabel from '../../page-objects/contentWithLabel';

export const BASE_SEARCH_PAGE_URL = '/search';
export const BASE_SEARCH_NEW_PAGE_URL = '/searchNew';

const getSearchPageURI = (query?: string) => {
    const uri = query ? `${BASE_SEARCH_PAGE_URL}?query=${query}` : BASE_SEARCH_PAGE_URL;

    return encodeURI(uri);
};

const getSearchNewPageURI = (query?: string) => {
    const uri = query ? `${BASE_SEARCH_NEW_PAGE_URL}?query=${query}` : BASE_SEARCH_NEW_PAGE_URL;

    return encodeURI(uri);
};

const getURIFromURL = (url: string) => {
    const currentUrl = new URL(url);

    return `${currentUrl.pathname}${currentUrl.search}`;
};

describe('ocrm-1582: Редирект на новый поиск', () => {
    it('При открытии страницы /searchNew c пустыми query параметрами происходит редирект на /search c пустыми параметрами', async function() {
        await login(getSearchNewPageURI(), this);

        this.browser.waitUntil(async () => getSearchPageURI() === getURIFromURL(await this.browser.getUrl()), {
            timeout: LONG_TIMEOUT_MS,
            timeoutMsg: 'Не дождались редиректа на страницу /search',
            interval: CHECK_INTERVAL,
        });
    });

    it('При открытии страницы /searchNew c заданными query параметрами происходит редирект на /search c такими же query параметрами', async function() {
        const query = 'test';

        await login(getSearchNewPageURI(query), this);

        this.browser.waitUntil(async () => getSearchPageURI(query) === getURIFromURL(await this.browser.getUrl()), {
            timeout: LONG_TIMEOUT_MS,
            timeoutMsg: 'Не дождались редиректа на страницу /search',
            interval: CHECK_INTERVAL,
        });
    });
});

describe('ocrm-1499: Поиск по несуществующим данным', () => {
    const query = 'test1234567890test09087654321test';

    it('При вводе поискового запроса с пустой выдачей должен отображаться текст о том, что ничего не найдено', async function() {
        await login(getSearchPageURI(), this);

        const searchInput = new SearchInput(this.browser);
        const submitSearchButton = new Button(
            this.browser,
            'body',
            '[data-ow-test-new-search-page="submit-search-button"]'
        );
        const noSearchFoundBlock = new SearchBlockWithText(
            this.browser,
            'body',
            '[data-ow-test-new-search-page="not-found"]'
        );

        await searchInput.isDisplayed();
        await searchInput.setValue(query);

        await submitSearchButton.isDisplayed();
        await submitSearchButton.clickButton();

        await noSearchFoundBlock.isDisplayed('Не дождались появления блока с текстом о том, что ничего не найдено');
        const noSearchFoundBlockText = await noSearchFoundBlock.getInnerText();

        expect(noSearchFoundBlockText).to.equal(
            'Ничего не найдено или недостаточно прав для просмотра результатов поиска'
        );

        const uri = getURIFromURL(await this.browser.getUrl());

        expect(uri).to.equal(getSearchPageURI(query));
    });

    it('При открытии страницы поиска с заранее заданным поисковым запросом с пустой выдачей должен отображаться текст о том, что ничего не найдено', async function() {
        await login(getSearchPageURI(query), this);

        const searchInput = new SearchInput(this.browser);
        const noSearchFoundBlock = new SearchBlockWithText(
            this.browser,
            'body',
            '[data-ow-test-new-search-page="not-found"]'
        );

        await searchInput.isDisplayed();
        await searchInput.waitUntilValueIsEmpty();
        const searchInputValue = await searchInput.getValue();

        expect(searchInputValue).to.equal(query);

        await noSearchFoundBlock.isDisplayed('Не дождались появления блока с текстом о том, что ничего не найдено');
        const noSearchFoundBlockText = await noSearchFoundBlock.getInnerText();

        expect(noSearchFoundBlockText).to.equal(
            'Ничего не найдено или недостаточно прав для просмотра результатов поиска'
        );
    });
});

describe('ocrm-1500: Открытие страницы нового поиска', () => {
    it('При пустом поисковом запросе должен отображаться текст: "Введите значение для поиска"', async function() {
        await login(getSearchPageURI(), this);

        const noSearchQueryBlock = new SearchBlockWithText(
            this.browser,
            'body',
            '[data-ow-test-new-search-page="no-search-query"]'
        );

        await noSearchQueryBlock.isDisplayed(
            'Не дождались появления блока с текстом о том, что нет значения для поиска'
        );

        const noSearchQueryElemText = await noSearchQueryBlock.getInnerText();

        expect(noSearchQueryElemText).to.equal('Введите значение для поиска');
    });
});

describe('ocrm-1501: Работает глобальный поиск', () => {
    it('При поиске по названию тикета в таблице с выдачей присутствует строка с названием тикета', async function() {
        const tableValueRow = new ContentWithLabel(this.browser, 'body', '[data-ow-test-table-row-0="title"]');

        const createdTicketTitle = await createOutgoingTicket(this);

        this.browser.url(getSearchNewPageURI(createdTicketTitle));

        await tableValueRow.isDisplayed();

        const tableValueRowLinkText = await (await tableValueRow.link).getText();

        expect(tableValueRowLinkText).to.equal(createdTicketTitle, 'Строка таблицы не содержит нужного значения');
    });
});
