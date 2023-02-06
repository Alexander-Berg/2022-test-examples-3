/* eslint-disable no-await-in-loop */
import 'hermione';

import CardHeader from '../../../src/modules/jmfEntity/components/CardHeader/__pageObject__';
import {execScript, extractGid, login} from '../../helpers';
import Button from '../../page-objects/button';
import {ProfileType} from '../../helpers/login';

const PAGE_URL = '/entity/ticket@258664531/edit';
const TURN_ON_ORDER_HANDLING_SCRIPT = `def service = 'service@30013907'
api.bcp.edit(service, [api.bcp
    'forSpecificOrderHandling': true
])`;
const TURN_OFF_ORDER_HANDLING_SCRIPT = `def service = 'service@30013907'
api.bcp.edit(service, [
    'forSpecificOrderHandling': false
])`;

const GET_TICKET_SCRIPT = `api.db.of('ticket$firstLine')
        .withFilters(
            api.db.filters.eq('service', 'beruQuestion'),
            api.db.filters.or(
                api.db.filters.eq('status', 'registered'),
                api.db.filters.eq('status', 'reopened'),
            ),
        )
    .limit(1)
    .get()`;

/**
 * Проверяем, что:
 */
describe('ocrm-432: Если для обращений очереди запрещено менять заказ, то поле "Заказ" в обращении скрыто', () => {
    beforeEach(function() {
        return login('', this);
    });

    it('', async function() {
        const cancelButton = new Button(
            this.browser,
            'body',
            '[data-ow-test-jmf-card-toolbar-action="cancel-отменить"]'
        );

        const cardHeader = new CardHeader(this.browser, 'body', '[data-ow-test-card-header="default"]');
        const takeTicketButton = new Button(
            this.browser,
            'body',
            '[data-ow-test-jmf-card-toolbar-action="edit-в работу"]'
        );

        await execScript(this.browser, TURN_ON_ORDER_HANDLING_SCRIPT);
        await this.browser.refresh();
        const ticketRaw = await execScript(this.browser, GET_TICKET_SCRIPT);
        const ticketGid = extractGid(ticketRaw);

        await login(`/entity/${ticketGid}`, this, ProfileType.OPERATOR);
        await cardHeader.isDisplayed();
        await takeTicketButton.clickButton();

        await this.browser.url(`/entity/${ticketGid}/edit`);
        await cardHeader.isDisplayed();

        const orderEdit = await this.browser.$('[data-ow-test-properties-list-attribute="order"]');

        await orderEdit.waitForDisplayed({
            reverse: true,
            timeoutMsg: 'Поле заказ видно в режиме редактирования',
        });

        await cancelButton.clickButton();

        await cardHeader.isDisplayed();
        const editPropertiesButton = new Button(this.browser, 'body', '[data-ow-test-properties-list-action="edit"]');

        await editPropertiesButton.clickButton();
        const orderViewEdit = await this.browser.$(
            '[data-ow-test-modal-body] [data-ow-test-properties-list-attribute="order"]'
        );

        await orderViewEdit.waitForDisplayed({
            reverse: true,
            timeoutMsg: 'Поле заказ видно в режиме просмотра',
        });
        await login(PAGE_URL, this);
        await execScript(this.browser, TURN_OFF_ORDER_HANDLING_SCRIPT);
    });
});
