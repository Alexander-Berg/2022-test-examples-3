const PO = require('../../../../../page-objects/pages/candidate');

const options = {
    tolerance: 5,
    antialiasingTolerance: 5
}

describe('Коммуникация с кандидатом / Внешняя коммуникация', function() {
    it('Добавление сообщения', function() {
        return this.browser
            .conditionalLogin('marat')
            .preparePage('', '/candidates/85700404/messages')
            .disableAnimations('*')
            .disableFiScrollTo()
            .setFixedDateTime({
                year: 2019,
                month: 10,
                day: 24,
                hour: 0,
                min: 0,
                sec: 0,
            })
            .waitForPageLoad()
            .waitForVisible(PO.pageCandidate.tabs.menu())
            .assertView('candidate_tabs_menu', PO.pageCandidate.tabs.menu(), options)
            .click(PO.pageCandidate.paneTypeMessages.form.fMessageForm.fieldText())
            .waitForVisible(PO.pageCandidate.paneTypeMessages.formFocus())
            .assertView('candidate_external_message_form_focus', PO.pageCandidate.paneTypeMessages.formFocus())
            .setSFieldValue(PO.pageCandidate.paneTypeMessages.form.fMessageForm.fieldSubject(), 'Тестовая тема')
            .setSFieldValue(PO.pageCandidate.paneTypeMessages.form.fMessageForm.fieldText(), 'Тестовый текст')
            .assertView('candidate_external_message_form_filled', PO.pageCandidate.paneTypeMessages.formFocus())
            .click(PO.pageCandidate.paneTypeMessages.form.fMessageForm.submit())
            .waitForHidden(PO.pageCandidate.paneTypeMessages.formFocus())
            .assertView('candidate_external_messages_pane_new', [
                PO.pageCandidate.tabs.menu(),
                PO.pageCandidate.paneTypeMessages.form(),
                PO.pageCandidate.paneTypeMessages.messages.list.firstFMessage(),
            ]);
    });
});
