import {makeSuite, makeCase} from 'ginny';

/**
 * Тесты на блок Linker.
 * @param {PageObject.Linker|PageObject.PopularRecipes} linker
 */
export default makeSuite('Блок перелинковки. Фильтрация.', {
    feature: 'Блок перелинковки',
    story: {
        'При переходе по ссылке': {
            'должны быть выбраны указанные фильтры': makeCase({
                id: 'marketfront-2872',
                issue: 'MARKETVERSTKA-29644',
                async test() {
                    const {glfilter} = await this.linker.getLinkParamsByIndex(1);
                    const filterId = glfilter.split(':')[0];
                    await this.linker.clickItemByIndex(1);

                    return this.browser.allure.runStep('Проверяем, что фильтр выбран', async () => {
                        const statusFilter = await this.browser.getAttribute(`[data-filter-id="${filterId}"] input`, 'checked');
                        return this.expect(statusFilter).to.be.equal('true', 'Фильтр выбран');
                    });
                },
            }),
        },
    },
});
