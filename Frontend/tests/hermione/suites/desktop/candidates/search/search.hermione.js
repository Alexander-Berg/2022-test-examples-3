const PO = require('../../../../page-objects/pages/candidates');

/**
 * Общая подготовка перед каждым тест-кейсом
 * @param {Object} browser
 * @returns {Object}
 */
function prepareBrowser(browser) {
    return browser
        .conditionalLogin('marat')
        .preparePageExtended('/candidates/', [2019, 10, 24, 0, 0, 0], 'candidates/1')
        .disableAnimations('*')
        .disableFiScrollTo();
}

describe('Кандидат / Страница Поиска', function() {
    it('Проверка внешнего вида страницы', function() {
        return this.browser
            .then(() => prepareBrowser(this.browser))
            .waitForVisible(PO.pageCandidates())
            .assertView('candidates_page_header', PO.pageCandidates.header())
            .assertView('candidates_page_search', PO.pageCandidates.search())
            .assertView('candidates_page_message', PO.pageCandidates.message())
            .click(PO.pageCandidates.toggleFilterButton())
            .assertView('candidates_page_open_filter', PO.pageCandidates());
    });
    it('Проверка кнопки Создать', function() {
        return this.browser
            .then(() => prepareBrowser(this.browser))
            .waitForVisible(PO.pageCandidates())
            .click(PO.pageCandidates.createButton())
            .assertUrl('/candidates/create/');
    });
    it('Проверка примера', function() {
        return this.browser
            .then(() => prepareBrowser(this.browser))
            .waitForVisible(PO.pageCandidates())
            .click(PO.pageCandidates.message.exampleLink())
            .assertUrl('/candidates/?text=Иван%20Петров')
            .waitForVisible(PO.pageCandidates.table.row())
            .assertView('candidates_page_example', PO.pageCandidates());
    });
    it('Проверка информационного попапа \'Профессия\'', function() {
        return this.browser
            .then(() => prepareBrowser(this.browser))
            .waitForVisible(PO.pageCandidates())
            .click(PO.pageCandidates.toggleFilterButton())
            .waitForVisible(PO.pageCandidates.filter.rowTypeProfessions.info())
            .click(PO.pageCandidates.filter.rowTypeProfessions.info())
            .patchStyle(PO.popup2Visible.fInfoBody(), {
                boxShadow: 'none',
            })
            .assertView('popup_professions_info', PO.popup2Visible.fInfoBody())
            .click(PO.popup2Visible.fInfoCloser())
            .waitForHidden(PO.popup2Visible());
    });
    it('Проверка информационного попапа \'Средняя оценка Skype от\'', function() {
        return this.browser
            .then(() => prepareBrowser(this.browser))
            .waitForVisible(PO.pageCandidates())
            .click(PO.pageCandidates.toggleFilterButton())
            .waitForVisible(PO.pageCandidates.filter.rowTypeSkypeInterviewsAvgGrade.info())
            .click(PO.pageCandidates.filter.rowTypeSkypeInterviewsAvgGrade.info())
            .patchStyle(PO.popup2Visible.fInfoBody(), {
                boxShadow: 'none',
            })
            .assertView('popup_skype_interviews_avg_grade_info', PO.popup2Visible.fInfoBody())
            .click(PO.popup2Visible.fInfoCloser())
            .waitForHidden(PO.popup2Visible());
    });
    it('Проверка фильтра по профессии', function() {
        return this.browser
            .then(() => prepareBrowser(this.browser))
            .waitForVisible(PO.pageCandidates())
            .click(PO.pageCandidates.toggleFilterButton())
            .waitForVisible(PO.pageCandidates.filter.fieldTypeProfessions.input())
            .setSuggestValue({
                block: PO.pageCandidates.filter.fieldTypeProfessions.input(),
                menu: PO.professionsSuggest.items(),
                text: 'разработчик интерфейсов',
                item: PO.professionsSuggest.first(),
            })
            .waitForVisible(PO.pageCandidates.filterInformer())
            .click(PO.pageCandidates.search.button())
            .waitForVisible(PO.pageCandidates.table.row())
            .assertView('candidates_page_professions', PO.pageCandidates())
            .click(PO.pageCandidates.filter.fieldTypeProfessions.deletePlateButton())
            .waitForHidden(PO.pageCandidates.filterInformer());
    });
    it('Проверка фильтра по навыкам', function() {
        return this.browser
            .then(() => prepareBrowser(this.browser))
            .waitForVisible(PO.pageCandidates())
            .click(PO.pageCandidates.toggleFilterButton())
            .waitForVisible(PO.pageCandidates.filter.fieldTypeSkills.input())
            .setSuggestValue({
                block: PO.pageCandidates.filter.fieldTypeSkills.input(),
                menu: PO.skillsSuggest.items(),
                text: 'javascript',
                item: PO.skillsSuggest.javascript(),
            })
            .click(PO.pageCandidates.search.button())
            .waitForVisible(PO.pageCandidates.table.row())
            .assertView('candidates_page_skills', PO.pageCandidates());
    });
    it('Проверка фильтра по тегам', function() {
        return this.browser
            .then(() => prepareBrowser(this.browser))
            .waitForVisible(PO.pageCandidates())
            .click(PO.pageCandidates.toggleFilterButton())
            .waitForVisible(PO.pageCandidates.filter.fieldTypeTags.input())
            .setSuggestValue({
                block: PO.pageCandidates.filter.fieldTypeTags.input(),
                menu: PO.tagsSuggest.items(),
                text: 'productschool2018',
                item: PO.tagsSuggest.first(),
            })
            .click(PO.pageCandidates.search.button())
            .waitForVisible(PO.pageCandidates.table.row())
            .assertView('candidates_page_tags', PO.pageCandidates());
    });
    it('Проверка фильтра по средней оценке Skype', function() {
        return this.browser
            .then(() => prepareBrowser(this.browser))
            .waitForVisible(PO.pageCandidates())
            .click(PO.pageCandidates.toggleFilterButton())
            .waitForVisible(PO.pageCandidates.filter.fieldTypeSkypeInterviewsAvgGrade())
            .setSelectValue({
                block: PO.pageCandidates.filter.fieldTypeSkypeInterviewsAvgGrade(),
                menu: PO.skypeInterviewsAvgGradeSelect(),
                item: PO.skypeInterviewsAvgGradeSelect.specialist4(),
            })
            .click(PO.pageCandidates.search.button())
            .waitForVisible(PO.pageCandidates.table.row())
            .assertView('candidates_page_skype_interviews_avg_grade', PO.pageCandidates());
    });
    it('Проверка фильтра по средней оценке секций', function() {
        return this.browser
            .then(() => prepareBrowser(this.browser))
            .waitForVisible(PO.pageCandidates())
            .click(PO.pageCandidates.toggleFilterButton())
            .waitForVisible(PO.pageCandidates.filter.fieldTypeOnSiteInterviewsAvgGrade())
            .setSelectValue({
                block: PO.pageCandidates.filter.fieldTypeOnSiteInterviewsAvgGrade(),
                menu: PO.onSiteInterviewsAvgGradeSelect(),
                item: PO.onSiteInterviewsAvgGradeSelect.specialist4(),
            })
            .click(PO.pageCandidates.search.button())
            .waitForVisible(PO.pageCandidates.table.row())
            .assertView('candidates_page_on_site_interviews_avg_grade', PO.pageCandidates());
    });
    it('Проверка фильтра без nohire', function() {
        return this.browser
            .then(() => prepareBrowser(this.browser))
            .waitForVisible(PO.pageCandidates())
            .click(PO.pageCandidates.toggleFilterButton())
            .waitForVisible(PO.pageCandidates.filter.fieldTypeWithoutNohire.checkbox())
            .click(PO.pageCandidates.filter.fieldTypeWithoutNohire.checkbox())
            .click(PO.pageCandidates.search.button())
            .waitForVisible(PO.pageCandidates.table.row())
            .assertView('candidates_page_without_nohire', PO.pageCandidates());
    });
    it('Проверка фильтра без действующих сотрудников', function() {
        return this.browser
            .then(() => prepareBrowser(this.browser))
            .waitForVisible(PO.pageCandidates())
            .click(PO.pageCandidates.toggleFilterButton())
            .waitForVisible(PO.pageCandidates.filter.fieldTypeIgnoreEmployees.checkbox())
            .click(PO.pageCandidates.filter.fieldTypeIgnoreEmployees.checkbox())
            .click(PO.pageCandidates.search.button())
            .waitForVisible(PO.pageCandidates.table.row())
            .assertView('candidates_page_ignore_employees', PO.pageCandidates());
    });
    it('Проверка фильтра по городу рассмотрения', function() {
        return this.browser
            .then(() => prepareBrowser(this.browser))
            .waitForVisible(PO.pageCandidates())
            .click(PO.pageCandidates.toggleFilterButton())
            .waitForVisible(PO.pageCandidates.filter.fieldTypeTargetCities.input())
            .setSuggestValue({
                block: PO.pageCandidates.filter.fieldTypeTargetCities.input(),
                menu: PO.targetCitiesSuggest.items(),
                text: 'москва',
                item: PO.targetCitiesSuggest.first(),
            })
            .click(PO.pageCandidates.search.button())
            .waitForVisible(PO.pageCandidates.table.row())
            .assertView('candidates_page_target_cities', PO.pageCandidates());
    });
    it('Проверка фильтра по городу кандидата', function() {
        return this.browser
            .then(() => prepareBrowser(this.browser))
            .waitForVisible(PO.pageCandidates())
            .click(PO.pageCandidates.toggleFilterButton())
            .waitForVisible(PO.pageCandidates.filter.fieldTypeCity())
            .setSFieldValue(PO.pageCandidates.filter.fieldTypeCity(), 'москва')
            .click(PO.pageCandidates.search.button())
            .waitForVisible(PO.pageCandidates.table.row())
            .assertView('candidates_page_city', PO.pageCandidates());
    });
    it('Проверка фильтра кандидатов в работе', function() {
        return this.browser
            .then(() => prepareBrowser(this.browser))
            .waitForVisible(PO.pageCandidates())
            .click(PO.pageCandidates.toggleFilterButton())
            .waitForVisible(PO.pageCandidates.filter.fieldTypeIsActive.radioButtonNo())
            .click(PO.pageCandidates.filter.fieldTypeIsActive.radioButtonNo())
            .click(PO.pageCandidates.search.button())
            .waitForVisible(PO.pageCandidates.table.row())
            .assertView('candidates_page_is_active_false', PO.pageCandidates());
    });
    it('Проверка фильтра по учебному заведению', function() {
        return this.browser
            .then(() => prepareBrowser(this.browser))
            .waitForVisible(PO.pageCandidates())
            .click(PO.pageCandidates.toggleFilterButton())
            .waitForVisible(PO.pageCandidates.filter.fieldTypeInstitution())
            .setSFieldValue(PO.pageCandidates.filter.fieldTypeInstitution(), 'мгу')
            .click(PO.pageCandidates.search.button())
            .waitForVisible(PO.pageCandidates.table.row())
            .assertView('candidates_page_institution', PO.pageCandidates());
    });
    it('Проверка фильтра по работодателю', function() {
        return this.browser
            .then(() => prepareBrowser(this.browser))
            .waitForVisible(PO.pageCandidates())
            .click(PO.pageCandidates.toggleFilterButton())
            .waitForVisible(PO.pageCandidates.filter.fieldTypeEmployer())
            .setSFieldValue(PO.pageCandidates.filter.fieldTypeEmployer(), 'google')
            .click(PO.pageCandidates.search.button())
            .waitForVisible(PO.pageCandidates.table.row())
            .assertView('candidates_page_employer', PO.pageCandidates());
    });
    it('Проверка фильтра по рекрутеру', function() {
        return this.browser
            .then(() => prepareBrowser(this.browser))
            .waitForVisible(PO.pageCandidates())
            .click(PO.pageCandidates.toggleFilterButton())
            .waitForVisible(PO.pageCandidates.filter.fieldTypeResponsibles())
            .setSuggestValue({
                block: PO.pageCandidates.filter.fieldTypeResponsibles.input(),
                menu: PO.responsiblesSuggest.items(),
                text: 'user3993',
                item: PO.responsiblesSuggest.first(),
            })
            .click(PO.pageCandidates.search.button())
            .waitForVisible(PO.pageCandidates.table.row())
            .assertView('candidates_page_responsibles', PO.pageCandidates());
    });
    it('Проверка фильтра по дате создания', function() {
        return this.browser
            .then(() => prepareBrowser(this.browser))
            .waitForVisible(PO.pageCandidates())
            .click(PO.pageCandidates.toggleFilterButton())
            .waitForVisible(PO.pageCandidates.filter.fieldTypeCreatedGte())
            .setSFieldValue(PO.pageCandidates.filter.fieldTypeCreatedGte(), '06-30-2019')
            .waitForVisible(PO.pageCandidates.filter.fieldTypeCreatedLte())
            .setSFieldValue(PO.pageCandidates.filter.fieldTypeCreatedLte(), '08-30-2019')
            .click(PO.pageCandidates.search.button())
            .waitForVisible(PO.pageCandidates.table.row())
            .assertView('candidates_page_created_gte_created_lte', PO.pageCandidates());
    });
    it('Проверка фильтра по дате обновления', function() {
        return this.browser
            .then(() => prepareBrowser(this.browser))
            .waitForVisible(PO.pageCandidates())
            .click(PO.pageCandidates.toggleFilterButton())
            .waitForVisible(PO.pageCandidates.filter.fieldTypeModifiedGte())
            .setSFieldValue(PO.pageCandidates.filter.fieldTypeModifiedGte(), '06-30-2019')
            .waitForVisible(PO.pageCandidates.filter.fieldTypeModifiedLte())
            .setSFieldValue(PO.pageCandidates.filter.fieldTypeModifiedLte(), '08-30-2019')
            .click(PO.pageCandidates.search.button())
            .waitForVisible(PO.pageCandidates.table.row())
            .assertView('candidates_page_modified_gte_modified_lte', PO.pageCandidates());
    });
    it('Проверка сортировки', function() {
        return this.browser
            .then(() => prepareBrowser(this.browser))
            .waitForVisible(PO.pageCandidates())
            .click(PO.pageCandidates.toggleFilterButton())
            .waitForVisible(PO.pageCandidates.filter.fieldTypeSort())
            .setSelectValue({
                block: PO.pageCandidates.filter.fieldTypeSort(),
                menu: PO.sortSelect(),
                item: PO.sortSelect.fromOldToNew(),
            })
            .click(PO.pageCandidates.search.button())
            .waitForVisible(PO.pageCandidates.table.row())
            .assertView('candidates_page_sort_from_old_to_new', PO.pageCandidates());
    });
    it('Проверка фильтра по данным из поисковой строки', function() {
        return this.browser
            .then(() => prepareBrowser(this.browser))
            .waitForVisible(PO.pageCandidates.search.input())
            .setValue(PO.pageCandidates.search.input(), 'сулохин')
            .click(PO.pageCandidates.search.button())
            .waitForVisible(PO.pageCandidates.table.row())
            .assertView('candidates_page_search_input', PO.pageCandidates());
    });
    it('Проверка нескольких фильтров сразу', function() {
        return this.browser
            .then(() => prepareBrowser(this.browser))
            .waitForVisible(PO.pageCandidates())
            .click(PO.pageCandidates.toggleFilterButton())
            .waitForVisible(PO.pageCandidates.filter.fieldTypeProfessions.input())
            .setSuggestValue({
                block: PO.pageCandidates.filter.fieldTypeProfessions.input(),
                menu: PO.professionsSuggest.items(),
                text: 'Разработчик бекэнда',
                item: PO.professionsSuggest.first(),
            })
            .setSuggestValue({
                block: PO.pageCandidates.filter.fieldTypeProfessions.input(),
                menu: PO.professionsSuggest.items(),
                text: 'инженер по тестированию',
                item: PO.professionsSuggest.first(),
            })
            .waitForVisible(PO.pageCandidates.filter.fieldTypeOnSiteInterviewsAvgGrade())
            .setSelectValue({
                block: PO.pageCandidates.filter.fieldTypeOnSiteInterviewsAvgGrade(),
                menu: PO.onSiteInterviewsAvgGradeSelect(),
                item: PO.onSiteInterviewsAvgGradeSelect.intern(),
            })
            .waitForVisible(PO.pageCandidates.filter.fieldTypeIgnoreEmployees.checkbox())
            .click(PO.pageCandidates.filter.fieldTypeIgnoreEmployees.checkbox())
            .waitForVisible(PO.pageCandidates.filter.fieldTypeTargetCities.input())
            .setSuggestValue({
                block: PO.pageCandidates.filter.fieldTypeTargetCities.input(),
                menu: PO.targetCitiesSuggest.items(),
                text: 'москва',
                item: PO.targetCitiesSuggest.first(),
            })
            .setSuggestValue({
                block: PO.pageCandidates.filter.fieldTypeTargetCities.input(),
                menu: PO.targetCitiesSuggest.items(),
                text: 'санкт-петербург',
                item: PO.targetCitiesSuggest.first(),
            })
            .waitForVisible(PO.pageCandidates.filterInformer())
            .click(PO.pageCandidates.search.button())
            .waitForVisible(PO.pageCandidates.table.row())
            .assertView('candidates_page_multiple_filters', PO.pageCandidates());
    });
    it('Проверка пагинации', function() {
        return this.browser
            .then(() => prepareBrowser(this.browser))
            .waitForVisible(PO.pageCandidates())
            .click(PO.pageCandidates.message.exampleLink())
            .waitForVisible(PO.pageCandidates.table.pager.secondPage())
            .click(PO.pageCandidates.table.pager.secondPage())
            .waitForVisible(PO.pageCandidates.table.row())
            .assertView('candidates_page_pagination', PO.pageCandidates());
    });
    it('Проверка ссылки с предустановленными параметрами', function() {
        return this.browser
            .then(() => prepareBrowser(this.browser))
            .preparePageExtended(
                '/candidates/?filter=yes&professions=3&professions=1&on_site_interviews_avg_grade=1&target_cities=1&target_cities=2&ignore_employees=true',
                [2019, 10, 24, 0, 0, 0],
                'candidates/1'
            )
            .waitForVisible(PO.pageCandidates.filter.fieldTypeProfessions.input()) // ждем пока фильтры появятся
            .waitForVisible(PO.pageCandidates.table.row())
            .assertView('candidates_page_link_params', PO.pageCandidates());
    });
});
