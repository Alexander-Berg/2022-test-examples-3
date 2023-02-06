const PO = require('../../../../page-objects/pages/vacancy');

/**
 * Открывает страницу создания вакансии
 * @param {Object} browser
 * @param {String} key new|replacement|internship
 * @param {String} url урл создания вакансии
 * @returns {Object}
 */
function openUrl(browser, key, url) {
    return browser
        .conditionalLogin('mop')
        .preparePage('', url)
        .waitForVisible(PO.vacancyForm())
        .disableAnimations('*')
        .disableFixedPosition()
        .assertView(`${key}_plain`, PO.vacancyFormView());
}

/**
 * Открывает страницу создания вакансии
 * и заполняет все одинаковые поля для разных типов вакансий
 * @param {Object} browser
 * @param {String} key new|replacement|internship
 * @param {String} url урл создания вакансии
 * @returns {Object}
 */
function openAndFillCommonFields(browser, key, url) {
    return browser
        .then(() => openUrl(browser, key, url))
        .setReactSFieldValue(PO.vacancyForm.title(), 'Новая вакансия', 'input')
        .addReactSuggestValue({
            block: PO.vacancyForm.department(),
            text: 'контест',
            position: 1,
            clickToFocus: true,
        })
        .waitForVisible(PO.vacancyForm.head())
        .addReactSuggestValue({
            block: PO.vacancyForm.hiringManager(),
            text: 'user3993',
            position: 1,
            clickToFocus: true,
        })
        .scroll(PO.vacancyForm.profType())
        .addReactSuggestValue({ block: PO.vacancyForm.profType(),
            text: '',
            position: 1,
            clickToFocus: true,
        })
        .waitForVisible(PO.vacancyForm.profession())
        .addReactSuggestValue({ block: PO.vacancyForm.profession(),
            text: '',
            position: 1,
            clickToFocus: true,
        })
        .setReactSFieldValue(PO.vacancyForm.workMode(), 1, 'checkbox-group')
        .setReactSFieldValue(PO.vacancyForm.workMode(), 2, 'checkbox-group')
        .addReactSuggestValue({
            block: PO.vacancyForm.locations(),
            text: 'Москва',
            position: 1,
            clickToFocus: true,
        })
        .addReactSuggestValue({
            block: PO.vacancyForm.valueStream(),
            text: 'Контест',
            position: 1,
            clickToFocus: true,
        })
    ;
}

/**
 * Создает вакансию
 * @param {Object} browser
 * @param {String} key new|replacement|internship
 * @returns {Object}
 */
function create(browser, key) {
    return browser
        .click(PO.vacancyFormView.header())
        .assertView(`${key}_filled`, PO.vacancyFormView())
        .click(PO.vacancyForm.submit())
        .waitForVisible(PO.vacancyFormView.buttonMore(), 10000)
        .assertView(`${key}_success`, PO.vacancyFormView.message());
}

describe('Вакансии / Создание вакансии', function() {
    it('Выбор типа вакансии', function() {
        return this.browser
            .then(() => openUrl(this.browser, 'empty', '/vacancies/create/'))
            .setReactSFieldValue(PO.vacancyFormView.vacancyType(), 'replacement', 'radio')
            .waitForVisible(PO.vacancyForm.insteadOf())
            .assertUrl('/vacancies/create/?type=replacement')
            .assertView('replacement_plain', PO.vacancyFormView())
            .setReactSFieldValue(PO.vacancyFormView.vacancyType(), 'internship', 'radio')
            .waitForHidden(PO.vacancyForm.insteadOf())
            .waitForVisible(PO.vacancyForm.department())
            .assertUrl('/vacancies/create/?type=internship')
            .assertView('internship_plain', PO.vacancyFormView())
            .setReactSFieldValue(PO.vacancyFormView.vacancyType(), 'new', 'radio')
            .waitForVisible(PO.vacancyForm.department())
            .assertUrl('/vacancies/create/?type=new')
            .assertView('new_plain', PO.vacancyFormView());
    });
    it('Создание новой вакансии', function() {
        const key = 'new';

        return this.browser
            .then(() => openAndFillCommonFields(this.browser, key, `/vacancies/create/?type=${key}`))
            .setReactSFieldValue(PO.vacancyForm.reason(), 'Закрытие вакансии', 'textarea')
            .setReactSFieldValue(PO.vacancyForm.proLevelMax(), 4, 'select')
            .setReactSFieldValue(PO.vacancyForm.wageSystem(), 'fixed', 'radio')
            .then(() => create(this.browser, key));
    });
    it('Создание вакансии на замену', function() {
        const key = 'replacement';

        return this.browser
            .then(() => openAndFillCommonFields(this.browser, key, '/vacancies/create/?type=replacement'))
            .setReactSFieldValue(PO.vacancyForm.proLevelMax(), 2, 'select')
            .addReactSuggestValue({
                block: PO.vacancyForm.insteadOf(),
                text: 'Арнольд Снеговой',
                position: 1,
                clickToFocus: true,
            })
            .setReactSFieldValue(PO.vacancyForm.replacementReason(), 'maternity_leave', 'radio')
            .waitForVisible(PO.vacancyForm.contractType())
            .setReactSFieldValue(PO.vacancyForm.contractType(), 'fixed', 'radio')
            .setReactSFieldValue(PO.vacancyForm.wageSystem(), 'fixed', 'radio')
            .then(() => create(this.browser, key));
    });
    it('Создание вакансии на стажировку', function() {
        const key = 'internship';

        return this.browser
            .then(() => openAndFillCommonFields(this.browser, key, '/vacancies/create/?type=internship'))
            .setReactSFieldValue(PO.vacancyForm.reason(), 'Закрытие вакансии', 'textarea')
            .then(() => create(this.browser, key));
    });
    it('Проверка ошибок формы', function() {
        const key = 'error';

        return this.browser
            .then(() => openUrl(this.browser, key, '/vacancies/create/?type=new'))
            .click(PO.vacancyForm.submit())
            .waitForVisible(PO.vacancyForm.errorMessage())
            .pause(2000) // scrolling
            .assertView(`${key}_form`, PO.vacancyFormView());
    });
});
