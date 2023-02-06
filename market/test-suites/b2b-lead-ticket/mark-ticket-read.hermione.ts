import 'hermione';
import {expect} from 'chai';

import {execScript, extractGid, login} from '../../helpers';
import {TIMEOUT_MS} from '../../constants';

const STAND_URL = '';
const MARK_READ_BUTTON_SELECTOR = '//span[contains(text(),"Пометить прочитанным")]';
const MARK_UNREAD_BUTTON_SELECTOR = '//span[contains(text(),"Пометить непрочитанным")]';
const GET_UNREAD_TICKET_GID_SCRIPT = `
    api.db.of('ticket$b2bLead')
    .withFilters(
      api.db.filters.eq('isRead', 'false'),
    )
    .limit(1)
    .get()
`;
const GET_READ_TICKET_GID_SCRIPT = `
    api.db.of('ticket$b2bLead')
    .withFilters(
      api.db.filters.eq('isRead', 'true'),
    )
    .limit(1)
    .get()
`;
const getTicketReadStatusScript = ticketGid => `
    api.db.get('${ticketGid}').isRead
`;

describe(`ocrm-1397: Обращения b2b лидов можно помечать`, () => {
    beforeEach(function() {
        return login(STAND_URL, this);
    });

    /**
     * План теста:
     * 1) Получаем gid прочитанного или непрочитанного тикета
     * 2) Открываем тикет и жмем на кнопку прочитано или не прочитано
     * 3) Проверяем что текст кнопки изменился и у самого тикета значение атрибута isRead сменилось
     */

    it('прочитанными.', async function() {
        const ticketRaw = await execScript(this.browser, GET_UNREAD_TICKET_GID_SCRIPT);
        const ticket = extractGid(ticketRaw);
        const ticketUrl = `entity/${ticket}`;
        const markReadButton = await this.browser.$(MARK_READ_BUTTON_SELECTOR);
        const markUnreadButton = await this.browser.$(MARK_UNREAD_BUTTON_SELECTOR);

        await this.browser.url(ticketUrl);
        await markReadButton.waitForClickable({
            timeout: TIMEOUT_MS,
            timeoutMsg: 'Кнопка "Пометить прочитанным" не стала кликабельной за 10 секунд',
        });
        await markReadButton.click();
        await markUnreadButton.waitForDisplayed({
            timeout: TIMEOUT_MS,
            timeoutMsg: 'Кнопка "Пометить прочитанным" не сменилась на кнопку "Пометить непрочитанным" за 10 секунд',
        });

        const ticketReadStatusRaw = await execScript(this.browser, getTicketReadStatusScript(ticket));
        const ticketReadStatus = extractGid(ticketReadStatusRaw);

        expect(ticketReadStatus).to.equal('true', 'Обращение не пометилось прочитанным');
    });

    it('непрочитанными.', async function() {
        const ticketRaw = await execScript(this.browser, GET_READ_TICKET_GID_SCRIPT);
        const ticket = extractGid(ticketRaw);
        const ticketUrl = `entity/${ticket}`;
        const markReadButton = await this.browser.$(MARK_READ_BUTTON_SELECTOR);
        const markUnreadButton = await this.browser.$(MARK_UNREAD_BUTTON_SELECTOR);

        await this.browser.url(ticketUrl);
        await markUnreadButton.waitForClickable({
            timeout: TIMEOUT_MS,
            timeoutMsg: 'Кнопка "Пометить непрочитанным" не стала кликабельной',
        });
        await markUnreadButton.click();
        await markReadButton.waitForDisplayed({
            timeout: TIMEOUT_MS,
            timeoutMsg: 'Кнопка "Пометить непрочитанным" не сменилась на кнопку "Пометить прочитанным"',
        });

        const ticketReadStatusRaw = await execScript(this.browser, getTicketReadStatusScript(ticket));
        const ticketReadStatus = extractGid(ticketReadStatusRaw);

        expect(ticketReadStatus).to.equal('false', 'Обращение не пометилось непрочитанным');
    });
});
