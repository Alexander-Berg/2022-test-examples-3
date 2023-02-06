import 'hermione';
import {expect} from 'chai';

import {login, turnOffExperiment} from '../../helpers';
import ContentWithLabel from '../../page-objects/contentWithLabel';

const PAGE_URL = '/entity/ticket@195679941';
const DATE_IN_RUSSIAN = '15 сент. в 14:21';

/**
 * Проверяем, что на странице обращения дата регистрации отображается на русском
 */
describe(`ocrm-1659: Дата регистрации обращения отображается на русском`, () => {
    beforeEach(function() {
        return login(PAGE_URL, this);
    });

    it('Проверяет совпадение отображения даты с ожидаемым', async function() {
        await turnOffExperiment(this.browser);
        const properties = new ContentWithLabel(this.browser, 'body', '[data-ow-test-content="properties-cardTop"]');

        await properties.isExisting();

        const registrationDate = await this.browser.$('[data-ow-test-attribute-container="registrationDate"] span');

        const dataIsExisting = await registrationDate.isExisting();

        expect(dataIsExisting).to.equal(true, 'Дата регистрации не отображается на странице');

        const date = await registrationDate.getText();

        expect(date).to.equal(DATE_IN_RUSSIAN, 'Дата не соответсвует ожидаемой');
    });
});
