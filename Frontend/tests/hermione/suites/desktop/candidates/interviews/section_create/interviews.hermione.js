const { setAjaxHash } = require('../../../../../helpers');

const PO = require('../../../../../page-objects/pages/candidate');

function openForm(browser) {
    return browser
        .conditionalLogin('marat')
        .preparePage('', '/candidates/200001122/interviews/')
        .waitForVisible(PO.interviewCreate())
        .assertView('create_button', PO.interviewCreate())
        .click(PO.interviewCreate())
        .waitForVisible(PO.interviewActionForm())
        .waitForVisible(PO.interviewActionForm.type())
        .execute(() => {
            let inputs = document.querySelectorAll('input');
            // выключаем проверку правописания
            inputs.forEach(input => input.setAttribute('spellcheck', 'false'));
        });
}

function fillForm(browser, { key, name, typePO }) {
    return browser
        .setSelectValue({
            block: PO.interviewActionForm.type(),
            menu: PO.interviewTypeSelect.menu(),
            item: typePO(),
        })
        .waitForVisible(PO.interviewActionForm.application())
        .setSelectValue({
            block: PO.interviewActionForm.application(),
            menu: PO.applicationSelect.menu(),
            item: PO.applicationSelect.vac50228(),
        })
        .setValue(PO.interviewActionForm.interview.input(), name)
        .setSuggestValue({
            block: PO.interviewActionForm.interviewer.input(),
            menu: PO.interviewerSuggest.items(),
            text: 'marat',
            item: PO.interviewerSuggest.marat(),
        })
        .setValue(PO.interviewActionForm.eventUrl.input(), 'https://calendar.tst.yandex-team.ru/event/?event_id=13434')
        .assertView(`${key}_form_filled`, PO.interviewActionForm());
}

function fillAaForm(browser, { key, typePO }) {
    return browser
        .setSelectValue({
            block: PO.interviewActionForm.type.button(),
            menu: PO.interviewTypeSelect.menu(),
            item: typePO(),
        })
        .waitForVisible(PO.interviewActionForm.typeAa())
        .waitForHidden(PO.interviewActionForm.interviewer.disabled())
        .setSelectValue({
            block: PO.interviewActionForm.interviewer.button(), //Указываем кнопку, тк она появляется позже родительского блока
            menu: PO.interviewerSelect.menu(),
            item: PO.interviewerSelect.markova(),
        })
        .setValue(PO.interviewActionForm.eventUrl.input(), 'https://calendar.tst.yandex-team.ru/event/?event_id=13434')
        .assertView(`${key}_form_filled`, PO.interviewActionForm());
}

function submitForm(browser, { key }) {
    return browser
        .execute(setAjaxHash, `${key}_after_added`)
        .click(PO.interviewActionForm.submit())
        .waitForHidden(PO.interviewActionForm())
        .waitForVisible(PO.interview())
        .waitForVisible(PO.event.date(), 3000)
        .click(PO.pageCandidate.user.header())
        .assertView(`${key}_new_interview_appeared`, PO.interview());
}

describe('Кандидат.Испытания', function() {
    describe('Создание секции', function() {
        it('Создание предварительной секции', function() {
            return this.browser
                .then(() => openForm(this.browser))
                .then(() => fillForm(this.browser, {
                    key: 'screening',
                    name: 'Предварительная секция',
                    typePO: PO.interviewTypeSelect.screening,
                }))
                .then(() => submitForm(this.browser, {
                    key: 'screening',
                }));
        });

        it('Создание стандартной секции', function() {
            return this.browser
                .then(() => openForm(this.browser))
                .then(() => fillForm(this.browser, {
                    key: 'regular',
                    name: 'Стандартная секция',
                    typePO: PO.interviewTypeSelect.regular,
                }))
                .then(() => submitForm(this.browser, {
                    key: 'regular',
                }));
        });

        it('Создание АА секции', function() {
            return this.browser
                .then(() => openForm(this.browser))
                .then(() => fillAaForm(this.browser, {
                    key: 'aa',
                    typePO: PO.interviewTypeSelect.aa,
                }))
                .then(() => submitForm(this.browser, {
                    key: 'aa',
                }));
        });
    });
});
