import {makeSuite, makeCase, mergeSuites} from 'ginny';

/**
 * Тесты на блок Footer.
 * @param {PageObject.Footer} footer
 */
export default makeSuite('Блок футер.', {
    story: mergeSuites(
        {
            'По умолчанию': {
                'должен присутствовать на странице.': makeCase({
                    id: 'm-touch-1823',
                    issue: 'MOBMARKET-6842',
                    environment: 'kadavr',
                    test() {
                        return this.browser.allure.runStep(
                            'Проверяем, что футер есть на странице',
                            () => this.footer.isExisting()
                                .should.eventually.to.be.equal(true, 'Футер отобразился на странице')
                        );
                    },
                }),
            },
        }
    ),
});
