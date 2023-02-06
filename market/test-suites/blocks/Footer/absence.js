import {makeSuite, makeCase} from 'ginny';

/**
 * Тесты на отсутствие блока Footer.
 * @param {PageObject.Footer} footer
 */
export default makeSuite('Блок футер.', {
    story: {
        'На странице, где блок скрыт': {
            'должен быть не виден.': makeCase({
                id: 'm-touch-2378',
                issue: 'MOBMARKET-9654',
                test() {
                    return this.browser.allure.runStep(
                        'Проверяем, что футер не виден',
                        () => this.footer.isVisible()
                            .should.eventually.to.be.equal(false, 'Футер не виден')
                    );
                },
            }),
        },
    },
});
