const PO = require('../../../../../page-objects/pages/application');

describe('Коммуникация с кандидатом / Внутренняя коммуникация', function() {
    it('Добавление сообщения', function() {
        return this.browser
            .conditionalLogin('marat')
            .preparePage('', '/applications/1003275/')
            .disableAnimations('*')
            .waitForVisible(PO.pageApplication.messageForm())
            .assertView('application_message_form', PO.pageApplication.messageForm())
            .click(PO.pageApplication.messageForm.textAreaControl())
            .assertView('application_message_form_focused', PO.pageApplication.messageForm())
            .setValue(PO.pageApplication.messageForm.textAreaControl(), 'Тестовый комментарий')
            .assertView('application_message_form_filled', PO.pageApplication.messageForm())
            .click(PO.pageApplication.messageForm.submit())
            .waitForHidden(PO.pageApplication.messageForm.submitDisabled())
            .waitForVisible(PO.pageApplication.messageForm.submit())
            .assertView('application_messages_updated', PO.pageApplication.messages());
    });
});
