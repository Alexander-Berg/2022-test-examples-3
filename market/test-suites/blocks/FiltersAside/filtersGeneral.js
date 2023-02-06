// flow

import {
    makeSuite,
    makeCase,
} from 'ginny';

/**
 * Тест на фильтры: блок фильтров виден, метрика отправляется
 * @property {PageObject.SearchFiltersAside} SearchFiltersAside
 * @params {route, description} -- путь до страницы и тип выдачи
 */
export default makeSuite('Видимость блока фильтров', {
    story: {
        'При открытии страницы блок фильтров виден': makeCase({
            async test() {
                await this.browser.yaOpenPage('market:search', this.params.route);
                return this.browser.allure.runStep(`Проверяем видимость блока фильтров: ${this.params.description}`,
                    () => this.filtersAside.isVisible()
                );
            },
        }),
    },
});
