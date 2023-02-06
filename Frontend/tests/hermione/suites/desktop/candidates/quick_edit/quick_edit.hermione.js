const path = require('path');

const PO = require('../../../../page-objects/pages/candidate');
const RPO = require('../../../../page-objects/react-pages/candidate');
const { setAjaxHash } = require('../../../../helpers');

const options = {
    tolerance: 5,
    antialiasingTolerance: 5,
};

function prepare(browser, page) {
    return browser
        .conditionalLogin('marat')
        .preparePage('', page)
        .disableAnimations('*')
        //Отключает position: fixed - требуется для корректной склейки скриншотов элементов, непомещающихся во viewport
        .disableFixedPosition()
        .waitForPageLoad()
        .waitForLoad();
}

describe('Кандидат / Быстрые правки', function() {
    const tags = [
        PO.pageCandidate.rightTab.tagsHeader(),
        PO.pageCandidate.rightTab.tags(),
    ];
    it('Правки тегов', function() {
        return prepare(this.browser, '/candidates/200015794/')
            .waitForVisible(PO.pageCandidate.rightTab.tags())
            .assertView('tags', tags)
            .waitForVisible(PO.pageCandidate.rightTab.tagsHeader.edit())
            .click(PO.pageCandidate.rightTab.tagsHeader.edit())
            .waitForVisible(RPO.candidateEditForm())
            .assertView('tags_edit_form', PO.tagsEditForm())
            .deleteReactSuggestValue({
                formHeader: RPO.candidateEditForm.header(),
                block: RPO.candidateEditForm.tagsField(),
                position: 1,
            })
            .addReactSuggestValue({ block: RPO.candidateEditForm.tagsField(), text: 'd', position: 1, clickToFocus: true })
            .assertView('tags_edit_form_changed', PO.tagsEditForm())
            .click(RPO.candidateEditForm.submit())
            .waitForHidden(PO.tagsEditForm())
            .assertView('tags_changed', tags);
    });
    it('Правки рекрутеров, проверка обязательных полей', function() {
        return prepare(this.browser, '/candidates/200017019/')
            .waitForVisible(PO.pageCandidate.status())
            .waitForPseudoContent(PO.pageCandidate.recruitersEdit.icon(), 'before')
            .assertView('recruiters', PO.pageCandidate.recruitersList(), options)
            .click(PO.pageCandidate.recruitersEdit())
            .waitForVisible(RPO.candidateEditForm())
            .assertView('recruiters_edit_form', PO.recruitersEditForm())
            .deleteReactSuggestValue({
                formHeader: RPO.candidateEditForm.header(),
                block: RPO.candidateEditForm.mainRecruiterField(),
                position: 1,
            })
            .assertView('recruiters_edit_form_changed', PO.recruitersEditForm())
            .click(RPO.candidateEditForm.submit())
            .waitForVisible(RPO.candidateEditForm.formError())
            .assertView('recruiters_form_error', PO.recruitersEditForm());
    });
    it('Правки рекрутеров, проверка ошибок', function() {
        return prepare(this.browser, '/candidates/200017019/')
            .waitForVisible(PO.pageCandidate.status())
            .waitForPseudoContent(PO.pageCandidate.recruitersEdit.icon(), 'before')
            .assertView('recruiters', PO.pageCandidate.recruitersList())
            .click(PO.pageCandidate.recruitersEdit())
            .waitForVisible(RPO.candidateEditForm())
            .assertView('recruiters_edit_form', PO.recruitersEditForm())
            .addReactSuggestValue({ block: RPO.candidateEditForm.recruitersField(), text: 'd', position: 1, clickToFocus: true })
            .assertView('recruiters_edit_form_changed', PO.recruitersEditForm())
            .click(RPO.candidateEditForm.submit())
            .waitForVisible(RPO.candidateEditForm.formError())
            .assertView('recruiters_form_error', PO.recruitersEditForm());
    });
    it('Правки рекрутеров', function() {
        return prepare(this.browser, '/candidates/200017019/')
            .waitForVisible(PO.pageCandidate.status())
            .waitForPseudoContent(PO.pageCandidate.recruitersEdit.icon(), 'before')
            .assertView('recruiters', PO.pageCandidate.recruitersList(), options)
            .click(PO.pageCandidate.recruitersEdit())
            .waitForVisible(RPO.candidateEditForm())
            .assertView('recruiters_edit_form', PO.recruitersEditForm())
            .deleteReactSuggestValue({
                formHeader: RPO.candidateEditForm.header(),
                block: RPO.candidateEditForm.mainRecruiterField(),
                position: 1,
            })
            .addReactSuggestValue({ block: RPO.candidateEditForm.mainRecruiterField(), text: 'annvas', position: 1, clickToFocus: true })
            .addReactSuggestValue({ block: RPO.candidateEditForm.recruitersField(), text: 'qazaq', position: 1, clickToFocus: true })
            .assertView('recruiters_edit_form_changed', PO.recruitersEditForm())
            .click(RPO.candidateEditForm.submit())
            .waitForHidden(PO.recruitersEditForm())
            .assertView('recruiters_changed', PO.pageCandidate.recruitersList());
    });
    it('Правки контактов, проверка обязательных полей', function() {
        const contacts = [
            PO.pageCandidate.rightTab.contactsHeader(),
            PO.pageCandidate.rightTab.contacts(),
        ];
        return prepare(this.browser, '/candidates/200017019/')
            .waitForVisible(PO.pageCandidate.rightTab.contactsHeader.edit())
            .assertView('contacts', contacts)
            .click(PO.pageCandidate.rightTab.contactsHeader.edit())
            .waitForVisible(RPO.candidateEditForm())
            .assertView('contacts_edit_form', PO.contactsEditForm())
            .setReactSFieldValue(RPO.candidateEditForm.fieldMainEmail(), '', 'input')
            .setReactSFieldValue(RPO.candidateEditForm.fieldMainPhone(), '', 'input')
            .setReactSFieldValue(RPO.candidateEditForm.fieldMainSkype(), '', 'input')
            .assertView('contacts_edit_form_changed', PO.contactsEditForm())
            .click(RPO.candidateEditForm.submit())
            .waitForVisible(RPO.candidateEditForm.formError())
            .assertView('contacts_form_error', PO.contactsEditForm());
    });
    it('Правки контактов, проверка ошибок', function() {
        const contacts = [
            PO.pageCandidate.rightTab.contactsHeader(),
            PO.pageCandidate.rightTab.contacts(),
        ];
        return prepare(this.browser, '/candidates/200017019/')
            .waitForVisible(PO.pageCandidate.rightTab.contactsHeader.edit())
            .assertView('contacts', contacts)
            .click(PO.pageCandidate.rightTab.contactsHeader.edit())
            .waitForVisible(RPO.candidateEditForm())
            .assertView('contacts_edit_form', PO.contactsEditForm())
            .setReactSFieldValue(RPO.candidateEditForm.fieldMainEmail(), 'ololo', 'input')
            .setReactSFieldValue(RPO.candidateEditForm.fieldMainPhone(), 'alala', 'input')
            .click(RPO.candidateEditForm.gridContacts.add())
            .click(RPO.candidateEditForm.gridContacts.add())
            .setReactSFieldValue(RPO.candidateEditForm.gridContacts.contact2.select(), 2, 'select')
            .setReactSFieldValue(RPO.candidateEditForm.gridContacts.contact2.input(), 'ololo', 'input')
            .click(RPO.candidateEditForm.gridContacts.add())
            .setReactSFieldValue(RPO.candidateEditForm.gridContacts.contact3.select(), 3, 'select')
            .setReactSFieldValue(RPO.candidateEditForm.gridContacts.contact3.input(), 'alala', 'input')
            .assertView('contacts_edit_form_changed', PO.contactsEditForm())
            .click(RPO.candidateEditForm.submit())
            .waitForVisible(RPO.candidateEditForm.formError())
            .assertView('contacts_form_error', RPO.candidateEditForm.content());
    });
    it('Правки контактов', function() {
        const contacts = [
            PO.pageCandidate.rightTab.contactsHeader(),
            PO.pageCandidate.rightTab.contacts(),
        ];
        return prepare(this.browser, '/candidates/200017019/')
            .waitForVisible(PO.pageCandidate.rightTab.contactsHeader.edit())
            .assertView('contacts', contacts)
            .click(PO.pageCandidate.rightTab.contactsHeader.edit())
            .waitForVisible(RPO.candidateEditForm())
            .waitForLoad()
            .assertView('contacts_edit_form', PO.contactsEditForm())
            .setReactSFieldValue(RPO.candidateEditForm.fieldMainEmail(), 'vasilypupkin@yandex.ru', 'input')
            .setReactSFieldValue(RPO.candidateEditForm.fieldMainPhone(), '+79266748807', 'input')
            .click(RPO.candidateEditForm.gridContacts.contact1.delete())
            .click(RPO.candidateEditForm.gridContacts.add())
            .setReactSFieldValue(RPO.candidateEditForm.gridContacts.contact1.input(), 'skype', 'input')
            .click(RPO.candidateEditForm.gridContacts.add())
            .setReactSFieldValue(RPO.candidateEditForm.gridContacts.contact2.select(), 2, 'select')
            .setReactSFieldValue(RPO.candidateEditForm.gridContacts.contact2.input(), '+79266748907', 'input')
            .click(RPO.candidateEditForm.gridContacts.add())
            .setReactSFieldValue(RPO.candidateEditForm.gridContacts.contact3.select(), 3, 'select')
            .setReactSFieldValue(RPO.candidateEditForm.gridContacts.contact3.input(), 'vasynia@gmail.ru', 'input')
            .click(RPO.candidateEditForm.gridContacts.add())
            .setReactSFieldValue(RPO.candidateEditForm.gridContacts.contact4.select(), 4, 'select')
            .setReactSFieldValue(RPO.candidateEditForm.gridContacts.contact4.input(), 'https://hh.ru/', 'input')
            .assertView('contacts_edit_form_changed', PO.contactsEditForm())
            .click(RPO.candidateEditForm.submit())
            .waitForHidden(PO.contactsEditForm())
            .assertView('contacts_changed', contacts);
    });
    it('Правки профессий, проверка обязательных полей', function() {
        const professions = [
            PO.pageCandidate.leftTab.professionsHeader(),
            PO.pageCandidate.leftTab.professions(),
        ];
        return prepare(this.browser, '/candidates/200017043/')
            .waitForVisible(PO.pageCandidate.leftTab.professionsHeader.edit())
            .assertView('professions', professions)
            .click(PO.pageCandidate.leftTab.professionsHeader.edit())
            .waitForVisible(RPO.candidateEditForm())
            .waitForLoad()
            .assertView('professions_edit_form', PO.professionsEditForm(), { antialiasingTolerance: 5 })
            .deleteReactSuggestValue({
                formHeader: RPO.candidateEditForm.header(),
                block: RPO.candidateEditForm.gridProfessions.professions1.profession(),
                position: 1,
            })
            .assertView('professions_edit_form_changed', PO.professionsEditForm())
            .click(RPO.candidateEditForm.submit())
            .waitForVisible(RPO.candidateEditForm.formError())
            .assertView('professions_form_error', PO.professionsEditForm());
    });
    it('Правки профессий', function() {
        const professions = [
            PO.pageCandidate.leftTab.professionsHeader(),
            PO.pageCandidate.leftTab.professions2(),
        ];
        return prepare(this.browser, '/candidates/200017043/')
            .waitForVisible(PO.pageCandidate.leftTab.professionsHeader.edit())
            .assertView('professions', professions)
            .click(PO.pageCandidate.leftTab.professionsHeader.edit())
            .waitForVisible(RPO.candidateEditForm())
            .waitForLoad()
            .assertView('professions_edit_form', PO.professionsEditForm())
            .click(RPO.candidateEditForm.gridProfessions.professions1.delete())
            .deleteReactSuggestValue({
                formHeader: RPO.candidateEditForm.header(),
                block: RPO.candidateEditForm.gridProfessions.professions1.profession(),
                position: 1,
            })
            .addReactSuggestValue({ block: RPO.candidateEditForm.gridProfessions.professions1.profession(), text: 'б', position: 1, clickToFocus: true })
            .setReactSFieldValue(RPO.candidateEditForm.gridProfessions.professions1.salary(), '123000', 'input')
            .click(RPO.candidateEditForm.gridProfessions.add())
            .addReactSuggestValue({ block: RPO.candidateEditForm.gridProfessions.professions2.profession(), text: 'р', position: 1, clickToFocus: true })
            .setReactSFieldValue(RPO.candidateEditForm.gridProfessions.professions2.salary(), '43000', 'input')
            .assertView('professions_edit_form_changed', PO.professionsEditForm())
            .execute(setAjaxHash, 'after_form_changed')
            .click(RPO.candidateEditForm.submit())
            .waitForHidden(PO.professionsEditForm())
            .assertView('professions_changed', professions);
    });
    it('Правки ФИО', function() {
        const userName = [
            PO.pageCandidate.user.userName(),
            PO.pageCandidate.user.userNameEdit(),
        ];
        return prepare(this.browser, '/candidates/106467219/')
            .waitForVisible(PO.pageCandidate.user())
            .assertView('fullName', userName)
            .click(PO.pageCandidate.user.userNameEdit())
            .waitForVisible(RPO.candidateEditForm())
            .assertView('fullName_edit_form', PO.fullNameEditForm())
            .setReactSFieldValue(RPO.candidateEditForm.firstName(), 'Именной', 'input')
            .setReactSFieldValue(RPO.candidateEditForm.middleName(), 'Отчествович', 'input')
            .setReactSFieldValue(RPO.candidateEditForm.lastName(), 'Фамиля', 'input')
            .assertView('fullName_edit_form_changed', PO.fullNameEditForm())
            .click(RPO.candidateEditForm.submit())
            .waitForHidden(PO.fullNameEditForm())
            .assertView('fullName_changed', userName);
    });
    it('Правки ФИО, проверка обязательных полей', function() {
        const userName = [
            PO.pageCandidate.user.userName(),
            PO.pageCandidate.user.userNameEdit(),
        ];
        return prepare(this.browser, '/candidates/106467219/')
            .waitForVisible(PO.pageCandidate.user())
            .assertView('fullName', userName)
            .click(PO.pageCandidate.user.userNameEdit())
            .waitForVisible(RPO.candidateEditForm())
            .assertView('fullName_edit_form', PO.fullNameEditForm())
            .setReactSFieldValue(RPO.candidateEditForm.lastName(), '', 'input')
            .assertView('fullName_edit_form_changed', PO.fullNameEditForm())
            .click(RPO.candidateEditForm.submit())
            .waitForVisible(RPO.candidateEditForm.formError())
            .assertView('fullName_form_error', PO.fullNameEditForm());
    });
    it('Правки рассмотрения', function() {
        const consideration = [
            PO.pageCandidate.considerationHeader(),
            PO.pageCandidate.considerationBody(),
        ];
        return prepare(this.browser, '/candidates/200016243/')
            .waitForVisible(PO.pageCandidate.considerationHeader.edit())
            .setFixedDateTime({
                year: 2020,
                month: 11,
                day: 2,
            })
            .assertView('consideration', consideration)
            .click(PO.pageCandidate.considerationHeader.edit())
            .waitForVisible(RPO.candidateEditForm())
            .assertView('consideration_edit_form', PO.considerationEditForm())
            .setReactSFieldValue(RPO.candidateEditForm.source(), 5, 'select')
            .setReactSFieldValue(RPO.candidateEditForm.sourceDescription(), 'Описание', 'textarea')
            .assertView('consideration_edit_form_changed', PO.considerationEditForm())
            .click(RPO.candidateEditForm.submit())
            .waitForHidden(PO.considerationEditForm())
            .assertView('consideration_changed', consideration);
    });
    it('Правки опыта работы', function() {
        const jobs = [
            PO.pageCandidate.jobsHeader(),
            PO.pageCandidate.jobsBody(),
        ];
        return prepare(this.browser, '/candidates/200016924/')
            .waitForVisible(PO.pageCandidate.jobsBody())
            .setFixedDateTime({
                year: 2021,
                month: 08,
                day: 2,
            })
            .assertView('jobs', jobs)
            .click(PO.pageCandidate.jobsHeader.edit())
            .waitForVisible(RPO.candidateEditForm())
            .assertView('jobs_edit_form', PO.jobsEditForm())
            .click(RPO.candidateEditForm.gridJobs.jobs1.delete())
            .setReactSFieldValue(RPO.candidateEditForm.gridJobs.jobs1.employer(), 'Лаборатория вымышленных корпораций', 'input')
            .setReactSFieldValue(RPO.candidateEditForm.gridJobs.jobs1.position(), 'Младший выдумыватель корпоративных прав', 'input')
            .setReactSFieldValue(RPO.candidateEditForm.gridJobs.jobs1.startMonth(), 4, 'input')
            .setReactSFieldValue(RPO.candidateEditForm.gridJobs.jobs1.startYear(), 2000, 'input')
            .setReactSFieldValue(RPO.candidateEditForm.gridJobs.jobs1.endMonth(), 5, 'input')
            .setReactSFieldValue(RPO.candidateEditForm.gridJobs.jobs1.endYear(), 2001, 'input')
            .setReactSFieldValue(RPO.candidateEditForm.gridJobs.jobs1.salaryEvaluation(), '50', 'input')
            .click(RPO.candidateEditForm.gridJobs.add())
            .setReactSFieldValue(RPO.candidateEditForm.gridJobs.jobs2.employer(), 'Работка не пыльная', 'input')
            .setReactSFieldValue(RPO.candidateEditForm.gridJobs.jobs2.position(), 'Престарший Ленин', 'input')
            .setReactSFieldValue(RPO.candidateEditForm.gridJobs.jobs2.startMonth(), 3, 'input')
            .setReactSFieldValue(RPO.candidateEditForm.gridJobs.jobs2.startYear(), 2000, 'input')
            .setReactSFieldValue(RPO.candidateEditForm.gridJobs.jobs2.endMonth(), 5, 'input')
            .setReactSFieldValue(RPO.candidateEditForm.gridJobs.jobs2.endYear(), 2000, 'input')
            .setReactSFieldValue(RPO.candidateEditForm.gridJobs.jobs2.salaryEvaluation(), '500', 'input')
            .assertView('jobs_edit_form_changed', PO.jobsEditForm())
            .click(RPO.candidateEditForm.submit())
            .waitForHidden(PO.jobsEditForm())
            .assertView('jobs_changed', jobs);
    });
    it('Правки образования, проверка ошибок', function() {
        const educations = [
            PO.pageCandidate.leftTab.educationsHeader(),
            PO.pageCandidate.leftTab.educations(),
        ];
        return prepare(this.browser, '/candidates/200017044/')
            .waitForVisible(PO.pageCandidate.leftTab.educationsHeader.edit())
            .assertView('educations', educations)
            .click(PO.pageCandidate.leftTab.educationsHeader.edit())
            .waitForVisible(RPO.candidateEditForm())
            .assertView('educations_edit_form', PO.educationsEditForm())
            .setReactSFieldValue(RPO.candidateEditForm.gridEducations.educations1.institution(), '', 'input')
            .setReactSFieldValue(RPO.candidateEditForm.gridEducations.educations1.endYear(), 'вася', 'input')
            .assertView('educations_edit_form_changed', PO.educationsEditForm())
            .click(RPO.candidateEditForm.submit())
            .waitForVisible(RPO.candidateEditForm.formError())
            .assertView('educations_form_error', PO.educationsEditForm());
    });
    it('Правки образования', function() {
        const educations = [
            PO.pageCandidate.leftTab.educationsHeader(),
            PO.pageCandidate.leftTab.educations(),
        ];
        return prepare(this.browser, '/candidates/200017044/')
            .waitForVisible(PO.pageCandidate.leftTab.educationsHeader.edit())
            .assertView('educations', educations)
            .click(PO.pageCandidate.leftTab.educationsHeader.edit())
            .waitForVisible(RPO.candidateEditForm())
            .assertView('educations_edit_form', PO.educationsEditForm())
            .click(RPO.candidateEditForm.gridEducations.educations1.delete())
            .setReactSFieldValue(RPO.candidateEditForm.gridEducations.educations1.institution(), 'Institution', 'input')
            .setReactSFieldValue(RPO.candidateEditForm.gridEducations.educations1.faculty(), 'Faculty', 'input')
            .setReactSFieldValue(RPO.candidateEditForm.gridEducations.educations1.degree(), '3', 'select')
            .setReactSFieldValue(RPO.candidateEditForm.gridEducations.educations1.endYear(), '2021', 'input')
            .click(RPO.candidateEditForm.gridEducations.add())
            .setReactSFieldValue(RPO.candidateEditForm.gridEducations.educations2.institution(), 'Institution1', 'input')
            .setReactSFieldValue(RPO.candidateEditForm.gridEducations.educations2.faculty(), 'Faculty1', 'input')
            .setReactSFieldValue(RPO.candidateEditForm.gridEducations.educations2.degree(), '4', 'select')
            .setReactSFieldValue(RPO.candidateEditForm.gridEducations.educations2.endYear(), '2022', 'input')
            .assertView('educations_edit_form_changed', PO.educationsEditForm())
            .execute(setAjaxHash, 'after_form_changed')
            .click(RPO.candidateEditForm.submit())
            .waitForHidden(PO.educationsEditForm())
            .assertView('educations_changed', educations);
    });
    it('Правки О Кандидате', function() {
        const personal = [
            PO.pageCandidate.leftTab.personalHeader(),
            PO.pageCandidate.leftTab.personal(),
        ];
        return prepare(this.browser, '/candidates/106467219/')
            .waitForVisible(PO.pageCandidate.leftTab.personalHeader.edit())
            .assertView('personal', personal)
            .click(PO.pageCandidate.leftTab.personalHeader.edit())
            .waitForVisible(RPO.candidateEditForm())
            .assertView('personal_edit_form', PO.personalEditForm())
            .setReactSFieldValue(RPO.candidateEditForm.docs(), path.join(__dirname, './doc.pdf'), 'attachment')
            .setReactSFieldValue(RPO.candidateEditForm.birthday(), '05.09.2021', 'date')
            .setReactSFieldValue(RPO.candidateEditForm.gender(), 'male', 'radio')
            .setReactSFieldValue(RPO.candidateEditForm.country(), 'Европпппппппппа', 'input')
            .setReactSFieldValue(RPO.candidateEditForm.city(), 'Омск', 'input')
            .addReactSuggestValue({ block: RPO.candidateEditForm.targetCities(), text: 'Минск', position: 1, clickToFocus: true })
            .assertView('personal_edit_form_changed', PO.personalEditForm())
            .execute(setAjaxHash, 'after_form_changed')
            .setFixedDateTime({
                year: 2020,
                month: 11,
                day: 2,
            })
            .click(RPO.candidateEditForm.submit())
            .waitForHidden(PO.personalEditForm())
            .assertView('personal_changed', personal);
    });
});
