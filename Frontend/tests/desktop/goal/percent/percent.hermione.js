const PO = require('../../../../page-objects');

describe('Человек-процент', function() {
    beforeEach(async function() {
        const browser = this.browser;

        await browser.loginToGoals();

        await browser.preparePage('teamoutline', '/okr?flags=percent&importance=all&periods=2021Q3&statuses=450%2C451%2C433%2C432%2C0%2C1%2C2%2C173%2C5%2C0%2C1%2C2&selectedItem=58689&goal=58692');
        await browser.waitForVisible(PO.goal.info.details.teamTab());
        await browser.click(PO.goal.info.details.teamTab());
        await browser.yaShouldBeVisible(PO.goal.info.details.navWithTeam());
    });

    it('Внешний вид таба команда', async function() {
        const browser = this.browser;

        await browser.assertView('plain', PO.goal.info.details.okrParticipants());
    });

    it('Другие цели, в которых участвует человек', async function() {
        const LINK_URL = {
            pathname: '/okr',
            searchParams: {
                importance: 'all',
                periods: {
                    type: 'array',
                    value: '2021Q2,2021Q3',
                },
                selectedItem: '58689',
                goal: '55394',
                statuses: {
                    type: 'array',
                    value: '0,0,1,1,2,2,5,173,432,433,450,451',
                },
            },
        };
        const browser = this.browser;

        await browser.moveToObject(PO.goal.info.details.participants.second.loadValue());

        await browser.waitForVisible(PO.goal.info.details.participants.second.popup.secondGoal());

        await browser.assertView('other-goals-popup', PO.goal.info.details.participants.second.popup(), { dontMoveCursor: true });

        await browser.moveToObject(PO.goal.info.details.participants.second.popup.secondGoal());

        await browser.assertView('other-goals-popup-with-hover', PO.goal.info.details.participants.second.popup(), { dontMoveCursor: true });

        await browser.moveToObject(PO.goal.info.details.participants.second.popup.secondGoal());
        await browser.yaCheckHref(
            PO.goal.info.details.participants.second.popup.secondGoal(),
            LINK_URL,
        );
    });

    // TODO: https://st.yandex-team.ru/GOALS-2025
    // eslint-disable-next-line
    it.skip('Редактирование таба команда', async function() {
        const browser = this.browser;

        await browser.click(PO.goal.info.details.participants.editButton());

        await browser.waitForVisible(PO.goal.info.details.participants.editForm());
        await browser.assertView('initial-form', PO.goal.info.details.participants.editForm());
        await browser.click(PO.goal.info.details.participants.editForm.addButton());

        await browser.yaSuggestChooseItem(
            PO.goal.info.details.participants.editForm.lastRow.userSuggest,
            PO.goal.info.details.participants.editForm.lastRow.userSuggest,
            'а',
            '@',
        );
        await browser.yaSuggestChooseItem(
            PO.goal.info.details.participants.editForm.lastRow.specialtySuggest,
            PO.goal.info.details.participants.editForm.lastRow.specialtySuggest,
            'разр',
            'разр',
        );
        await browser.click(PO.goal.info.details.participants.editForm.lastRow.percentInput());
        await browser.yaKeyPress('91');
        await browser.assertView('filled-form', PO.goal.info.details.participants.editForm());

        await browser.click(PO.goal.info.details.participants.editForm.saveButton());
        await browser.waitForVisible(PO.goal.info.details.participants.first());

        await browser.assertView('changed-tab', PO.goal.info.details.participants());
    });
});
