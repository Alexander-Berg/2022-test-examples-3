import 'hermione';

import {login, execScript, turnOffExperiment, extractGid} from '../../helpers';
import {CHECK_INTERVAL, LONG_TIMEOUT_MS, TIMEOUT_MS} from '../../constants';
import ContentWithLabel from '../../page-objects/contentWithLabel';
import AttributePopup from '../../page-objects/attributePopup';
import Comment from '../../page-objects/comment';

const CLOSE_TICKET_BUTTON_SELECTOR = '//button//*[text() = "Завершить"]';
const STATUS_FIELD_SELECTOR = '[data-ow-test-attribute-container="status"]';
const GET_TICKET_SCRIPT = `
    def ticket = api.db.of('ticket$beru').withFilters {
    eq('service','beruQuestion')
    eq('archived', false)
    eq('status','registered')
    }.withOrders(api.db.orders.desc('creationTime')).limit(1).get()
    api.bcp.edit(ticket, ['status':'processing'])
`;

const getUrlAndGoToTicketPage = async browser => {
    const rawTicketGid = await execScript(browser, GET_TICKET_SCRIPT);
    const ticketGid = extractGid(rawTicketGid);
    const ticketEditPageUri = `entity/${ticketGid}/edit`;

    await browser.url(ticketEditPageUri);
};

const closeTicket = async (browser, closeTicketButton, statusChangeReason) => {
    await closeTicketButton.waitForDisplayed({
        timeout: TIMEOUT_MS,
        timeoutMsg: 'Кнопка "Завершить" не появилась в заданный интервал.',
    });
    await closeTicketButton.click();
    await statusChangeReason.waitForDisplayed({
        timeout: TIMEOUT_MS,
        timeoutMsg: 'Список статусов в кнопке "Завершить" не появился в заданный интервал.',
    });
    await statusChangeReason.click();
};

const changeTicketStatus = async (browser, statusChangeReasonSelector) => {
    const closeTicketButton = await browser.$(CLOSE_TICKET_BUTTON_SELECTOR);
    const statusChangeReason = await browser.$(statusChangeReasonSelector);

    await getUrlAndGoToTicketPage(browser);
    await closeTicket(browser, closeTicketButton, statusChangeReason);
};

const changeTicketStatusWithCategoryAndComment = async (browser, statusChangeReasonSelector) => {
    const closeTicketButton = await browser.$(CLOSE_TICKET_BUTTON_SELECTOR);
    const statusChangeReason = await browser.$(statusChangeReasonSelector);
    const categories = new ContentWithLabel(browser, 'body', '[data-ow-test-attribute-container="categories"]');
    const popupCategories = new AttributePopup(browser, 'body', '[data-ow-test-popup]');
    const firstElement = new ContentWithLabel(browser, '[data-ow-test-popup]', '[data-ow-test-checkbox]');
    const comment = new Comment(browser, 'body', '[data-ow-test-content="comments"] [role="textbox"]');

    await getUrlAndGoToTicketPage(browser);
    await categories.isDisplayed();
    await categories.setValue('');
    await popupCategories.isDisplayed();
    await firstElement.click();
    await comment.addComment('comment');

    await closeTicket(browser, closeTicketButton, statusChangeReason);
};

const checkingTicketStatus = async (browser, ticketStatus, expectedStatus) => {
    return browser.waitUntil(async () => (await ticketStatus.getText()) === expectedStatus, {
        timeout: LONG_TIMEOUT_MS,
        timeoutMsg: 'Статус тикета либо не изменился, либо отличается от ожидаемого.',
        interval: CHECK_INTERVAL,
    });
};

describe('ocrm-550: Закрытие обращения со статусом "Спам"', async () => {
    beforeEach(function() {
        return login('/', this);
    });

    it('После смены статуса обращения у него действительно меняется статус на "Спам"', async function() {
        await turnOffExperiment(this.browser);

        const statusChangeReasonSelector = '//li[text()="Пометить спамом"]';
        const ticketStatus = await this.browser.$(STATUS_FIELD_SELECTOR);
        const expectedStatus = 'Спам';

        await changeTicketStatus(this.browser, statusChangeReasonSelector);

        await checkingTicketStatus(this.browser, ticketStatus, expectedStatus);
    });
});

describe('ocrm-551: Закрытие обращения со статусом "Решён"', async () => {
    beforeEach(function() {
        return login('/', this);
    });

    it('После смены статуса обращения у него действительно меняется статус на "Решён"', async function() {
        await turnOffExperiment(this.browser);

        const statusChangeReasonSelector = '//li[text()="Отправить ответ"]';
        const ticketStatus = await this.browser.$(STATUS_FIELD_SELECTOR);
        const expectedStatus = 'Решен';

        await changeTicketStatusWithCategoryAndComment(this.browser, statusChangeReasonSelector);

        await checkingTicketStatus(this.browser, ticketStatus, expectedStatus);
    });
});

describe('ocrm-552: Закрытие обращения со статусом "Не требует ответа"', async () => {
    beforeEach(function() {
        return login('/', this);
    });

    it('После смены статуса обращения у него действительно меняется статус на "Не требует ответа"', async function() {
        await turnOffExperiment(this.browser);

        const statusChangeReasonSelector = '//li[text()="Не требует ответа"]';
        const ticketStatus = await this.browser.$(STATUS_FIELD_SELECTOR);
        const expectedStatus = 'Не требует ответа';

        await changeTicketStatusWithCategoryAndComment(this.browser, statusChangeReasonSelector);

        await checkingTicketStatus(this.browser, ticketStatus, expectedStatus);
    });
});
