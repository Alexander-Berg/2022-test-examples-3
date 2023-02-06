import 'hermione';

import {expect} from 'chai';

import {createOutgoingCallTicket} from '../../helpers/createOutgoingTicket';
import AttributeValue from '../../page-objects/attributeValue';
import CardHeader from '../../../src/modules/jmfEntity/components/CardHeader/__pageObject__';

describe('ocrm-553: Ручное создание обращения исходящей телефонии', () => {
    it('В созданном тикете исходящей телефонии значения всех полей соответствуют введённым на странице создания обращения', async function() {
        const createdTicket = await createOutgoingCallTicket(this);

        const title = new CardHeader(this.browser, 'body', '[data-ow-test-card-header="default"]');

        await title.isDisplayed();
        const titleValue = await title.getTitleText();

        expect(titleValue).to.equal(
            createdTicket.title,
            'Название обращения со страницы отличается от названия со страницы создания'
        );

        const phoneField = new AttributeValue(this.browser, 'body', '[data-ow-test-attribute-container="clientPhone"]');

        await phoneField.isDisplayed();
        const phoneValue = (await phoneField.getText()).trim();

        expect(phoneValue).to.equal(
            createdTicket.phone,
            'Телефон со страницы отличается от телефона со страницы создания'
        );

        const serviceField = new AttributeValue(this.browser, 'body', '[data-ow-test-attribute-container="service"]');

        await serviceField.isDisplayed();
        const serviceValue = await serviceField.getLinkText();

        expect(serviceValue).to.equal(
            createdTicket.service,
            'Очередь со страницы отличается от очереди на странице создания'
        );
    });
});
