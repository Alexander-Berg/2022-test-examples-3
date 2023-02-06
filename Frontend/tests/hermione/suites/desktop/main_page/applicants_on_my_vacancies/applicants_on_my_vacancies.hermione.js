const PO = require('../../../../page-objects/pages/personal');

const regInterview = /\/candidates\/[0-9]+\/interviews[\/]?$/;
const regVacancy = /\/vacancies\/[0-9]+[\/]?$/;

function prepare(browser, tableContent) {
    return browser
        .conditionalLogin('marat')
        .preparePageExtended('/', [2019, 12, 13, 0, 0, 0], '/123')
        .waitForPageLoad()
        .disableAnimations('*')
        .disableFiScrollTo()
        .waitForVisible(tableContent)
        .waitForLoad()
        //Из-за использования дампов может возникнуть уведомление об отсутствии соединения
        //При клике на уведомление, оно пропадает
        .execute((selector)=>{
            const notification = document.querySelector(selector);
            if( notification ){ notification.click() }
        }, '.m-notification');
}

describe('Главная страница / Претенденты на мои вакансии', function() {
    const {
        fMyApplicants,
        fMyApplicants: {
            fApplicationsTable: {
                firstApplication,
                message,
            },
            fApplicationsTableLoadingYes,
        },
    } = PO.pagePersonal;

    it('Пустой список претендентов', function() {
        return prepare(this.browser, fMyApplicants())
            .waitForHidden(fApplicationsTableLoadingYes())
            .assertView('empty_applicants', fMyApplicants());
    });

    it('Одностраничный список претендентов', function() {
        return prepare(this.browser, firstApplication())
            .assertView('my_applicants', fMyApplicants())
            .staticElement(PO.sidePopup())
            .click(firstApplication.details.link())
            .waitForVisible(PO.fSidePopupContent.fApplication.fMessageForm.form())
            .assertView('my_applications_popup', PO.fSidePopupContent.fApplication());
    });

    it('Ссылка на кандидата', function() {
        return prepare(this.browser, firstApplication())
            .assertView('first_applicant', firstApplication())
            .click(firstApplication.candidate.link())
            .assertUrlReg(regInterview);
    });

    it('Ссылка на вакансию', function() {
        return prepare(this.browser, firstApplication())
            .assertView('first_applicant', firstApplication())
            .click(firstApplication.vacancy.link())
            .assertUrlReg(regVacancy);
    });

    it('Ссылка на борду', function() {
        return prepare(this.browser, firstApplication())
            .assertView('applicants_header', fMyApplicants.header())
            .assertAttribute(
                fMyApplicants.boardLink(),
                'href',
                /.*\/applications\/dashboard\/\?vacancies=52969&vacancies=53240&vacancies=53166&vacancies=53862&vacancies=52602&vacancies=53270&vacancies=52960&vacancies=53273&vacancies=53876&vacancies=51544&vacancies=53112&vacancies=53176&vacancies=53188&vacancies=52724&vacancies=52800&vacancies=53235&vacancies=53799&vacancies=53863&vacancies=53189&vacancies=53138&vacancies=53234&vacancies=53872/,
                true,
            );
    });

    it('Пагинация списка претендентов', function() {
        return prepare(this.browser, firstApplication())
            .assertView('applicants_list_page1', fMyApplicants())
            .click(fMyApplicants.fApplicationsTable.pager.page2())
            .waitUntil(() => {
                return this.browser
                    .getText(firstApplication.details.link())
                    .then(text => {
                        return text === 'APL 1004078';
                    });
            }, 5000, 'Не поменялась страница', 250)
            .assertView('applicants_list_page2', fMyApplicants());
    });

    it('Закрытие претендента', function() {
        return prepare(this.browser, firstApplication())
            .assertView('first_applicant', firstApplication())
            .click(firstApplication.closeAction())
            .waitForVisible(PO.bodyModal.button2Submit())
            .assertView('my_applications_open_close_modal', PO.bodyModal.content())
            .click(PO.bodyModal.fApplicationWorkflowRowTypeResolution())
            .click(PO.popup2Visible.menu.secondItem())
            .click(PO.bodyModal.button2Submit())
            .waitForHidden(PO.bodyModal.fApplicationWorkflowRowTypeResolution())
            .assertView('first_applicant_closed', firstApplication());
    });

    it('Фильтр на вакансию', function() {
        return prepare(this.browser, firstApplication())
            .assertView('my_applicants', fMyApplicants())
            .click(fMyApplicants.fApplicationFilter.vacancyFilter())
            .waitForVisible(PO.popup2Visible.menu.thirdItem())
            .click(PO.popup2Visible.menu.thirdItem())
            .waitForLoad()
            .waitForVisible(firstApplication())
            .assertView('applicants_filtered', fMyApplicants());
    });

    it('Фильтр на состояние', function() {
        return prepare(this.browser, firstApplication())
            .assertView('my_applicants', fMyApplicants())
            .click(fMyApplicants.fApplicationFilter.stageFilter.radioThird())
            .waitForLoad()
            .waitForVisible(firstApplication())
            .assertView('applicants_filtered', fMyApplicants());
    });
});
