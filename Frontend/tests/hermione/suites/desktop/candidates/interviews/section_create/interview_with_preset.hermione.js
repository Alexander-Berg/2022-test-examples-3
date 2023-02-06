const { setAjaxHash } = require('../../../../../helpers');

const PO = require('../../../../../page-objects/pages/candidate');
const IPO = require('../../../../../page-objects/pages/interview');

describe('Кандидат.Испытания', function() {
    describe('Создание секции', function() {
        it('C добавлением пресета задач', function() {
            return this.browser
                .conditionalLogin('marat')
                .preparePage('', '/candidates/200001122/interviews/')
                .waitForVisible(PO.interviewCreate())
                .click(PO.interviewCreate())
                .waitForVisible(PO.interviewActionForm())
                .waitForVisible(PO.interviewActionForm.type())
                .execute(() => {
                    let inputs = document.querySelectorAll('input');
                    // выключаем проверку правописания
                    inputs.forEach(input => input.setAttribute('spellcheck', 'false'));
                })
                .setSelectValue({
                    block: PO.interviewActionForm.type(),
                    menu: PO.interviewTypeSelect.menu(),
                    item: PO.interviewTypeSelect.screening(),
                })
                .waitForVisible(PO.interviewActionForm.application())
                .setSelectValue({
                    block: PO.interviewActionForm.application(),
                    menu: PO.applicationSelect.menu(),
                    item: PO.applicationSelect.vac50228(),
                })
                .setValue(PO.interviewActionForm.interview.input(), 'Предварительная секция')
                .setSuggestValue({
                    block: PO.interviewActionForm.interviewer.input(),
                    menu: PO.interviewerSuggest.items(),
                    text: hermione.ctx.testUsers.marat.username,
                    item: PO.interviewerSuggest.marat(),
                })
                .setSuggestValue({
                    block: PO.interviewActionForm.preset.input(),
                    menu: PO.presetSuggest.items(),
                    text: 'Пресет Марата',
                    item: PO.presetSuggest.preset(),
                })
                .setValue(PO.interviewActionForm.eventUrl.input(), 'https://calendar.tst.yandex-team.ru/event/?event_id=13434')
                .assertView('screening_form_filled', PO.interviewActionForm())
                .execute(setAjaxHash, 'screening_after_added')
                .click(PO.interviewActionForm.submit())
                .waitForHidden(PO.interviewActionForm())
                .waitForVisible(PO.interview())
                .waitForVisible(PO.event.date(), 3000)
                .click(PO.pageCandidate.user.header())
                .assertView('screening_new_interview_appeared', PO.interview())
                .click(PO.interview.title())
                .waitForVisible(IPO.assignmentsList.first())
                // компенсация отрицательного маржина
                .patchStyle(IPO.assignmentsList(), { paddingLeft: '30px' })
                .assertView('assignments_in_list', IPO.assignmentsList());
        });
    //});
    });
});
