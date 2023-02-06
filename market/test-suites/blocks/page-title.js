import {makeCase, makeSuite} from 'ginny';

/**
 * Тесты на title html-страницы.
 */
export default makeSuite('Title страницы.', {
    feature: 'Title страницы',
    story: {
        'По умолчанию': {
            'должен быть равен указанной строке.': makeCase({
                issue: 'MARKETVERSTKA-27671',
                id: 'marketfront-2314',
                params: {
                    expectedTitle: 'Ожидаемое значение для title страницы.',
                    matchMode: 'Флаг для переключения режима сравнения строк.',
                },
                defaultParams: {
                    matchMode: false,
                },
                test() {
                    const {expectedTitle, matchMode} = this.params;
                    const testMessage = `Title открытой страницы должен быть равен значению "${expectedTitle}"`;

                    const resultPromise = this.browser.allure.runStep(
                        'Получаем title открытой страницы',
                        () => this.browser.getTitle()
                            .then(pageTitle => this.browser.allure.runStep(
                                `открытая страница имеет title "${pageTitle}"`,
                                () => pageTitle)
                            )
                    );

                    // INFO: если флаг взведен, то проверяем полученный title переданным регулярным выражением
                    if (matchMode) {
                        return resultPromise
                            .then(pageTitle => this.browser.allure.runStep(
                                `Проверяем полученный title на соответствие регулярному выражению ${expectedTitle}`,
                                () => pageTitle
                            ))
                            .then(pageTitle => expectedTitle.test(pageTitle))
                            .should.eventually.be.equal(true, testMessage);
                    }

                    // INFO: в противном случае (по умолчанию) просто сравниваем строки
                    return resultPromise.should.eventually.be.equal(expectedTitle, testMessage);
                },
            }),
        },
    },
});
