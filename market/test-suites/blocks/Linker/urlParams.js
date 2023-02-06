import {makeSuite, makeCase} from 'ginny';

/**
 * Тесты на блок Linker.
 * @param {PageObject.Linker|PageObject.PopularRecipes} linker
 * @param {PageObject.Headline} headline
 */
export default makeSuite('Блок перелинковки. Параметры в урле.', {
    feature: 'Блок перелинковки',
    story: {
        'При переходе по ссылке': {
            'в урле должны содержаться необходимые параметры': makeCase({
                id: 'marketfront-1756',
                issue: 'MARKETVERSTKA-26382',
                params: {
                    query: 'Параметры, которые должны содержаться в урле',
                },
                async test() {
                    await this.linker.clickItemByIndex(1);

                    return this.browser.allure.runStep(
                        'Проверяем, что параметры содержатся в урле',
                        async () => {
                            const {query} = await this.browser.yaParseUrl();
                            return this.expect(query)
                                .to.deep.include(this.params.query, 'Все параметры содержатся в урле');
                        }
                    );
                },
            }),
        },
    },
});
