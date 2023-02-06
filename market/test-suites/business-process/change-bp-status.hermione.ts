import 'hermione';
import {expect} from 'chai';

import {execScript, extractGid, login} from '../../helpers';
import Button from '../../page-objects/button';
import SelectValue from '../../page-objects/selectValue';
import AttributeValue from '../../page-objects/attributeValue';
import {CASES} from './config';
import {VERY_LONG_TIMEOUT_MS} from '../../constants';

const STAND_URL = '';
const getTicketWithStartingBpStatus = (ticketMetaClass, bpTitleBefore): string => `
    def status = api.db.of('bpStatus')
                   .withFilters(
                     api.db.filters.eq('title', '${bpTitleBefore}'),
                     api.db.filters.eq('archived', false),
                   )
                   .limit(1)
                   .get()

    api.db.of('${ticketMetaClass}')
      .withFilters(
        api.db.filters.eq('currentStatus', status),
        api.db.filters.eq('status', 'registered'),
      )
    .limit(1)
    .get()
`;
const getBpTitleAfterRoot = bpTitleAfter => `[data-ow-test-select-option="${bpTitleAfter}"]`;

CASES.forEach(testCase => {
    describe(testCase.name, () => {
        beforeEach(function() {
            return login(STAND_URL, this);
        });

        /**
         * План теста:
         * 1) Получаем gid тикета со статусом БП = Открыт
         * 2) Открываем тикет и меняем статус БП
         * 3) Проверяем что статус БП сменился
         */

        it('приводит к смене статуса БП.', async function() {
            const ticketMetaClass = testCase.metaClass;
            const bpTitleBefore = testCase.bpTitleBefore;
            const bpTitleAfter = testCase.bpTitleAfter;
            const ticketRaw = await execScript(
                this.browser,
                getTicketWithStartingBpStatus(ticketMetaClass, bpTitleBefore)
            );
            const ticket = extractGid(ticketRaw);
            const ticketUrl = `entity/${ticket}`;
            const selectStatus = new SelectValue(this.browser, 'body', getBpTitleAfterRoot(bpTitleAfter));
            const editBbPropertiesButton = new Button(
                this.browser,
                '[data-ow-test-content="properties-bp"]',
                '[data-ow-test-properties-list-action="edit"]'
            );
            const currentStatusSelector = new Button(
                this.browser,
                '[data-ow-test-modal-body]',
                '[data-ow-test-attribute-container="currentStatus"]'
            );
            const saveButton = new Button(
                this.browser,
                '[data-ow-test-modal-controls]',
                '[data-ow-test-properties-list-action="save"]'
            );
            const currentStatus = new AttributeValue(
                this.browser,
                '[data-ow-test-content="properties-bp"]',
                '[data-ow-test-attribute-container="currentStatus"]'
            );

            await this.browser.url(ticketUrl);
            await editBbPropertiesButton.isDisplayed(undefined, VERY_LONG_TIMEOUT_MS);
            await editBbPropertiesButton.clickButton();
            await currentStatusSelector.clickButton();
            await selectStatus.clickSelectValue();
            await saveButton.clickButton();
            await saveButton.isNotDisplayed();
            const currentStatusValue = await currentStatus.getLinkText();

            expect(currentStatusValue).to.equal(bpTitleAfter, 'Статус БП не сменился');
        });
    });
});
