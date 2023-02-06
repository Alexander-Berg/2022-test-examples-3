import {makeSuite, makeCase} from 'ginny';

/**
 * Тесты на отсутствие виджета Header.
 * @param {PageObject.Header} header
 */
export default makeSuite('Блок шапки.', {
    story: {
        'На странице, где блок скрыт': {
            'должен быть не виден.': makeCase({
                id: 'm-touch-2377',
                issue: 'MOBMARKET-9653',
                test() {
                    return this.browser.allure.runStep(
                        'Проверяем, что шапка не видна',
                        () => this.header.isVisible()
                            .should.eventually.to.be.equal(false, 'Шапка не видна')
                    );
                },
            }),
        },
    },
});
