/* global MockDate */
const path = require('path');

const PO = require('../../../../../../page-objects/react-pages/create-interview-round.js');
const CPO = require('../../../../../../page-objects/react-pages/candidate');

describe('Кандидат.Испытания / Серия секций', function() {
    it('Проверка ошибки дубликатов', function() {
        return this.browser
            .conditionalLogin('marat')
            .preparePage('', '/candidates/200015794/interviews/create')
            .waitForVisible(PO.interviewCreateView.error())
            .assertView('duplicates_page', PO.interviewCreateView());
    });
    it('Проверка тайтла', function() {
        return this.browser
            .conditionalLogin('marat')
            .preparePage('', '/candidates/200017091/interviews/create')
            .waitForVisible(PO.interviewCreateView.candidate.id())
            .assertView('candidate_info', PO.interviewCreateView.candidate())
            .click(PO.interviewCreateView.candidate.id())
            .assertUrl('/candidates/200017091/');
    });
    it('Проверка ошибок', function() {
        return this.browser
            .conditionalLogin('marat')
            .preparePageExtended('/candidates/200017091/interviews/create/', [2020, 10, 13, 0, 0, 0])
            .waitForVisible(PO.interviewCreateForm())
            // чтобы сфотать звездочки обязательных полей, у них отрицательный маржин
            .patchStyle(PO.interviewCreateForm(), { padding: '10px' })
            .assertView('interview_round_form', PO.interviewCreateForm())
            .deleteReactSuggestValue({
                formHeader: PO.interviewCreateForm.header(),
                block: PO.interviewCreateForm.office(),
                position: 1,
            })
            .deleteReactSuggestValue({
                formHeader: PO.interviewCreateForm.header(),
                block: PO.interviewCreateForm.timezone(),
                position: 1,
            })
            .click(PO.interviewCreateForm.gridTimeSlots.timeSlot1.delete())
            .setReactSFieldValue(PO.interviewCreateForm.email(), '', 'input')
            .click(PO.interviewCreateForm.submit())
            .pause(2000) // scrolling
            .assertView('interview_round_form_errors1', PO.interviewCreateForm())
            .setReactSFieldValue(PO.interviewCreateForm.type(), 2, 'select')
            .setReactSFieldValue(PO.interviewCreateForm.interviewSlots.interviewSlot1.title(), '', 'input')
            .click(PO.interviewCreateForm.submit())
            .pause(2000) // scrolling
            .assertView('interview_round_form_errors2', PO.interviewCreateForm())
            .setReactSFieldValue(PO.interviewCreateForm.interviewSlots.interviewSlot1.application(), 2, 'select')
            .pause(500) // срабатывает предзаполнение данных полей
            .click(PO.interviewCreateForm.submit())
            .pause(2000) // scrolling
            .assertView('interview_round_form_errors3', PO.interviewCreateForm());
    });
    it('Создание предварительной секции', function() {
        const fixedOptions = {
            tolerance: 5,
            antialiasingTolerance: 5,
            screenshotDelay: 500,
        };
        return this.browser
            .conditionalLogin('marat')
            .preparePageExtended('/candidates/200017091/interviews/create/', [2021, 11, 30, 0, 0, 0])
            .waitForVisible(PO.interviewCreateForm())
            // чтобы сфотать звездочки обязательных полей, у них отрицательный маржин
            .patchStyle(PO.interviewCreateForm(), { padding: '10px' })
            .assertView('interview_round_form', PO.interviewCreateForm(), fixedOptions)
            .addReactSuggestValue({ block: PO.interviewCreateForm.office(), text: 'Морозов', position: 1, clickToFocus: true })
            .addReactSuggestValue({ block: PO.interviewCreateForm.timezone(), text: 'Minsk', position: 1, clickToFocus: true })
            .setReactSFieldValue(PO.interviewCreateForm.isAnyTime(), true, 'checkbox')
            .setReactSFieldValue(PO.interviewCreateForm.type(), 2, 'select')
            .waitForVisible(PO.interviewCreateForm.interviewSlots.interviewSlot1())
            .assertView('preliminary_section', PO.interviewCreateForm.interviewSlots.interviewSlot1(), fixedOptions)
            .setReactSFieldValue(PO.interviewCreateForm.interviewSlots.interviewSlot1.application(), 2, 'select')
            .setReactSFieldValue(PO.interviewCreateForm.interviewSlots.interviewSlot1.title(), 'Название секции', 'input')
            // Если у нас замокано время, перестает работать debounce lodash'а и отваливаются все саджесты.
            // Возможно, стоит в самом addReactSuggestValue восстанавливать текущее время а потом сетать мок заново.
            .execute(function() {
                MockDate.reset();
            })
            .addReactSuggestValue({ block: PO.interviewCreateForm.interviewSlots.interviewSlot1.additionalInterviewers(), text: 'o', position: 1, clickToFocus: true })
            .addReactSuggestValue({ block: PO.interviewCreateForm.interviewSlots.interviewSlot1.additionalInterviewers(), text: 'q', position: 1, clickToFocus: true })
            .assertView('interviewers', PO.interviewCreateForm.interviewSlots.interviewSlot1.interviewerTables(), fixedOptions)
            .click(PO.interviewCreateForm.interviewSlots.interviewSlot1.interviewerTables.delete())
            .assertView('interviewers2', PO.interviewCreateForm.interviewSlots.interviewSlot1.interviewerTables(), fixedOptions)
            .addReactSuggestValue({ block: PO.interviewCreateForm.interviewSlots.interviewSlot1.preset(), text: 'a', position: 1, clickToFocus: true })
            .setReactSFieldValue(PO.interviewCreateForm.comment(), 'Комментарий', 'textarea')
            .setReactSFieldValue(PO.interviewCreateForm.email(), 'olgakozlova.web@gmail.com', 'input')
            .setReactSFieldValue(PO.interviewCreateForm.templates.categories(), 2, 'select')
            .setReactSFieldValue(PO.interviewCreateForm.templates.options(), 2, 'select')
            .click(PO.interviewCreateForm.templates.apply())
            .setReactSFieldValue(PO.interviewCreateForm.signatures.options(), 2, 'select')
            .click(PO.interviewCreateForm.signatures.apply())
            .assertView('interview_round_form_filled', PO.interviewCreateForm(), fixedOptions)
            .click(PO.interviewCreateForm.submit())
            .waitForHidden(PO.interviewCreateForm())
            .assertUrl('/candidates/200017091/interviews/')
            .waitForVisible(CPO.interviewRound())
            .assertView('interview_request', CPO.interviewRound(), fixedOptions);
    });
    it('Создание стандартной секции', function() {
        return this.browser
            .conditionalLogin('marat')
            .preparePageExtended('/candidates/200017091/interviews/create/', [2022, 03, 15, 0, 0, 0])
            .waitForVisible(PO.interviewCreateForm())
            // чтобы сфотать звездочки обязательных полей, у них отрицательный маржин
            .patchStyle(PO.interviewCreateForm(), { padding: '10px' })
            .assertView('interview_round_form', PO.interviewCreateForm())
            .addReactSuggestValue({ block: PO.interviewCreateForm.office(), text: 'Морозов', position: 1, clickToFocus: true })
            .addReactSuggestValue({ block: PO.interviewCreateForm.timezone(), text: 'Minsk', position: 1, clickToFocus: true })
            .setReactSFieldValue(PO.interviewCreateForm.gridTimeSlots.timeSlot1.date(), '09.04.2022', 'date')
            .setReactSFieldValue(PO.interviewCreateForm.gridTimeSlots.timeSlot1.start(), '15:20', 'time')
            .setReactSFieldValue(PO.interviewCreateForm.gridTimeSlots.timeSlot1.end(), '18:09', 'time')
            .setReactSFieldValue(PO.interviewCreateForm.type(), 3, 'select')
            .assertView('usual_sections', PO.interviewCreateForm.interviewSlots())
            // Если у нас замокано время, перестает работать debounce lodash'а и отваливаются все саджесты.
            // Возможно, стоит в самом addReactSuggestValue восстанавливать текущее время а потом сетать мок заново.
            .execute(function() {
                MockDate.reset();
            })
            .setReactSFieldValue(PO.interviewCreateForm.interviewSlots.interviewSlot1.application(), 2, 'select')
            .waitForVisible(PO.interviewCreateForm.interviewSlots.interviewSlot1.interviewerTables.loadMore())
            .assertView('interviewers', PO.interviewCreateForm.interviewSlots.interviewSlot1.interviewerTables())
            .click(PO.interviewCreateForm.interviewSlots.interviewSlot1.interviewerTables.loadMore())
            .waitForHidden(PO.interviewCreateForm.interviewSlots.interviewSlot1.interviewerTables.loadMore())
            .assertView('interviewers2', PO.interviewCreateForm.interviewSlots.interviewSlot1.interviewerTables())
            .click(PO.interviewCreateForm.interviewSlots.interviewSlot1.interviewerTables.checkbox())
            .addReactSuggestValue({ block: PO.interviewCreateForm.interviewSlots.interviewSlot1.additionalInterviewers(), text: 'o', position: 1, clickToFocus: true })
            .assertView('interviewers3', PO.interviewCreateForm.interviewSlots.interviewSlot1.interviewerTables())
            .click(PO.interviewCreateForm.interviewSlots.add())
            .assertView('usual_sections2', [PO.interviewCreateForm.interviewSlots(), PO.interviewCreateForm.sequenceInfo()])
            .setReactSFieldValue(PO.interviewCreateForm.interviewSlots.interviewSlot2.type(), '2', 'select')
            .setReactSFieldValue(PO.interviewCreateForm.interviewSlots.interviewSlot2.aaType(), '2', 'select')
            .setReactSFieldValue(PO.interviewCreateForm.interviewSlots.interviewSlot2.isCode(), 'true', 'radio')
            .assertView('aa_section', PO.interviewCreateForm.interviewSlots.interviewSlot2())
            .setReactSFieldValue(PO.interviewCreateForm.ordering(), 2, 'select')
            .setReactSFieldValue(PO.interviewCreateForm.lunchDuration(), 1, 'select')
            .setReactSFieldValue(PO.interviewCreateForm.needNotify(), 'false', 'radio')
            .assertView('interview_round_form_filled', PO.interviewCreateForm())
            .click(PO.interviewCreateForm.submit())
            .waitForHidden(PO.interviewCreateForm())
            .assertUrl('/candidates/200017091/interviews/')
            .waitForVisible(CPO.interviewRound())
            .assertView('interview_request', CPO.interviewRound());
    });
    it('Создание финальной секции', function() {
        return this.browser
            .conditionalLogin('marat')
            .preparePageExtended('/candidates/200017091/interviews/create/', [2021, 11, 30, 0, 0, 0])
            .waitForVisible(PO.interviewCreateForm())
            // чтобы сфотать звездочки обязательных полей, у них отрицательный маржин
            .patchStyle(PO.interviewCreateForm(), { padding: '10px' })
            .assertView('interview_round_form', PO.interviewCreateForm())
            .addReactSuggestValue({ block: PO.interviewCreateForm.office(), text: 'Морозов', position: 1, clickToFocus: true })
            .addReactSuggestValue({ block: PO.interviewCreateForm.timezone(), text: 'Minsk', position: 1, clickToFocus: true })
            .setReactSFieldValue(PO.interviewCreateForm.gridTimeSlots.timeSlot1.isFullDay(), true, 'checkbox')
            .click(PO.interviewCreateForm.gridTimeSlots.add())
            .setReactSFieldValue(PO.interviewCreateForm.type(), 4, 'select')
            // Если у нас замокано время, перестает работать debounce lodash'а и отваливаются все саджесты.
            // Возможно, стоит в самом addReactSuggestValue восстанавливать текущее время а потом сетать мок заново.
            .execute(function() {
                MockDate.reset();
            })
            .setReactSFieldValue(PO.interviewCreateForm.interviewSlots.interviewSlot1.application(), 2, 'select')
            .addReactSuggestValue({ block: PO.interviewCreateForm.interviewSlots.interviewSlot1.additionalInterviewers(), text: 'o', position: 1, clickToFocus: true })
            .click(PO.interviewCreateForm.interviewSlots.add())
            .assertView('final_sections', [PO.interviewCreateForm.interviewSlots(), PO.interviewCreateForm.sequenceInfo()])
            .click(PO.interviewCreateForm.interviewSlots.interviewSlot2.delete())
            .assertView('final_sections2', [PO.interviewCreateForm.interviewSlots(), PO.interviewCreateForm.sequenceInfo()])
            .setReactSFieldValue(PO.interviewCreateForm.email(), 'olgakozlova.web@gmail.com', 'input')
            .setReactSFieldValue(PO.interviewCreateForm.subject(), 'Тема письма', 'input')
            .setReactSFieldValue(PO.interviewCreateForm.text(), 'Текст письма, приходите: ', 'macros-textarea')
            .click(PO.interviewCreateForm.text.addMacros())
            .setReactSFieldValue(PO.interviewCreateForm.attachments(), [
                path.join(__dirname, './userpic.png'),
                path.join(__dirname, './userpic2.png'),
            ], 'attachments')
            .assertView('interview_round_form_filled', PO.interviewCreateForm())
            .click(PO.interviewCreateForm.submit())
            .waitForHidden(PO.interviewCreateForm())
            .assertUrl('/candidates/200017091/interviews/')
            .waitForVisible(CPO.interviewRound())
            .assertView('interview_request', CPO.interviewRound());
    });
});
