const { setAjaxHash } = require('../../../../helpers');
const PO = require('../../../../page-objects/pages/interview');

describe('Секция / Задачи', function() {
    it('Добавление задачи из любимых', function() {
        return this.browser
            .conditionalLogin('marat')
            .preparePage('', '/interviews/33839/')
            .waitForVisible(PO.assignmentsList.addFavourite())
            .waitForVisible(PO.interviewPage.assigned.list())
            .assertView('assigned_interview', PO.interviewPage())
            .click(PO.assignmentsList.addFavourite())
            .waitForVisible(PO.problemsList.first())
            // компенсация отрицательного маржина
            .patchStyle(PO.tabs(), { padding: '0 30px' })
            .assertView('add_from_favourite', PO.tabs())
            .click(PO.problemsList.first.add())
            .pause(1000) // кнопка меняет цвета
            .assertView('after_add_click', PO.problemsList.first())
            .execute(setAjaxHash, 'after_add_click')
            .click(PO.tabs.firstTab())
            .waitForVisible(PO.assignmentsList.first())
            // компенсация отрицательного маржина
            .patchStyle(PO.assignmentsList(), { paddingLeft: '30px' })
            .assertView('assignments_in_list', PO.assignmentsList());
    });
    it('Добавление обычной задачи', function() {
        return this.browser
            .conditionalLogin('marat')
            .preparePage('', '/interviews/33839/')
            .waitForVisible(PO.assignmentsList.addFromCatalogue())
            .assertView('assigned_interview', PO.interviewPage())
            .click(PO.assignmentsList.addFromCatalogue())
            .waitForVisible(PO.problemsList.first())
            // компенсация отрицательного маржина
            .patchStyle(PO.tabs(), { padding: '0 30px' })
            .assertView('add_problem', [PO.tabs.tabs(), PO.problemsList.first()])
            .click(PO.problemsList.first.add())
            .pause(1000) // кнопка меняет цвета
            .assertView('after_add_click', PO.problemsList.first())
            .execute(setAjaxHash, 'after_add_click')
            .click(PO.tabs.firstTab())
            .waitForVisible(PO.assignmentsList.first())
            // компенсация отрицательного маржина
            .patchStyle(PO.assignmentsList(), { paddingLeft: '30px' })
            .assertView('assignments_in_list', PO.assignmentsList());
    });
});
