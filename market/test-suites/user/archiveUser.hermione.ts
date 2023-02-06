import 'hermione';
import {expect} from 'chai';

import {login, execScript, pause} from '../../helpers';
import Button from '../../page-objects/button';

const EMPLOYEE_GID = 'employee@114787185';
const UNARCHIVE_SCRIPT = `api.bcp.edit('${EMPLOYEE_GID}', ['status': 'active'])`;
const GET_EMPLOYEE_STATUS_SCRIPT = `api.db.get('${EMPLOYEE_GID}').status`;

describe(`ocrm-273: Архивирование пользователя"`, () => {
    beforeEach(function() {
        return login(`/entity/${EMPLOYEE_GID}`, this);
    });

    it(`Пользователь успешно архивируется`, async function() {
        await execScript(this.browser, UNARCHIVE_SCRIPT);

        const archiveButton = new Button(
            this.browser,
            'body',
            '[data-ow-test-jmf-card-toolbar-action="edit-архивировать"]'
        );

        this.browser.refresh();

        await archiveButton.isDisplayed();

        await archiveButton.clickButton();

        let employeeStatus;

        for (let i = 0; i < 20; i++) {
            // eslint-disable-next-line no-await-in-loop
            await pause(2000);
            // eslint-disable-next-line no-await-in-loop
            employeeStatus = await execScript(this.browser, GET_EMPLOYEE_STATUS_SCRIPT);

            if (employeeStatus === '"archived"') break;
        }

        await execScript(this.browser, UNARCHIVE_SCRIPT);

        expect(employeeStatus).to.equal('"archived"', 'Не удалось архивировать пользователя');
    });
});
