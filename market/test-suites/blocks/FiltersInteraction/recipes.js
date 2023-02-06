import url from 'url';
import {makeSuite, makeCase} from 'ginny';
import _ from 'lodash';
import {parseFilterQueryParams} from '@self/platform/spec/hermione/helpers/filters';


/**
 * Тест на взаимодействие фильтров c блоком «Часто ищут»
 * * @param {PageObject.PopularRecipes|PageObject.RecipesList} recipes
 */
export default makeSuite('Взаимодействие c блоком рецептов.', {
    feature: 'Фильтрация',
    environment: 'testing',
    story: {
        'При взаимодействии': {
            'переходит на нужную страницу, фильтр выбран и сохранился в урле.': makeCase({
                id: 'marketfront-609',
                issue: 'MARKETVERSTKA-24660',
                async test() {
                    const checkForQueryParamPresence = params =>
                        this.browser
                            .yaCheckUrlParams(params)
                            .should.eventually.to.be.equal(true, 'Проверяем наличие параметров');

                    const getFiltersQueryParamsFromLink = link => {
                        const query = url.parse(link, true).query;
                        delete query.track;

                        // @see https://github.com/nodejs/node/blob/master/lib/url.js#L242
                        // Создаётся Object.create(null), у которого нет прототипа,
                        // поэтому падает дальше внутри webdriverio методов при вызове toString
                        return {...query};
                    };

                    const checkFilterVisibility = id =>
                        this.browser.allure.runStep(`Проверяем, что фильтр ${id} виден`, () =>
                            this.browser
                                .isVisible(`[data-autotest-id="${id}"]`)
                                .should.eventually.to.be.equal(true, 'Фильтр виден на странице')
                        );

                    const checkFilterSelected = (id, value) =>
                        this.browser.allure.runStep(
                            `Проверяем, что фильтр ${id} ${value ? `со значение ${value} ` : ''}выбран`,
                            () => this.browser
                                .isSelected(`[id="${id}${value ? `_${value}` : ''}"]`)
                                .should.eventually.to.be.equal(true, 'Фильтр выбран')
                        );

                    const checkFilterValue = (id, type, value) =>
                        this.browser.allure.runStep(`Проверяем, что фильтр ${id} имеет значение ${value} в инпуте ${type}`, () =>
                            this.browser
                                .element(`[id="${id}${type}"]`)
                                .getValue()
                                .should.eventually.to.be.equal(value, 'Фильтр имеет правильное значение')
                        );

                    const checkFilters = filters => Promise
                        .all(
                            _.map(filters, (value, id) =>
                                this.browser.allure.runStep(
                                    `Проверяем, что фильтр с id равным ${id} и значением равным ${value} виден и выбран`,
                                    async () => {
                                        await checkFilterVisibility(id);

                                        // Диапозонный фильтр, кроме «Цены»
                                        if (value.includes('~')) {
                                            const [from, to] = value.split('~');

                                            if (from) {
                                                await checkFilterValue(id, 'from', from);
                                            }

                                            if (to) {
                                                await checkFilterValue(id, 'to', to);
                                            }

                                            return true;
                                        } else if (value === '1') {
                                            // Булевые (checkbox)
                                            return checkFilterSelected(id);
                                        }

                                        // Остальные
                                        return checkFilterSelected(id, value);
                                    }
                                )
                            )
                        )
                        .then(_.every)
                        .should.eventually.to.be.equal(true, 'Фильтры не выбраны или не видны');

                    const firstLinkUrl = await this.recipes.getItemUrlByIndex(1);
                    const filterQueryParams = getFiltersQueryParamsFromLink(firstLinkUrl);
                    const filters = parseFilterQueryParams(filterQueryParams);

                    await this.recipes.clickItemByIndex(1);
                    await this.browser.yaWaitForPageLoaded();
                    await checkForQueryParamPresence(filterQueryParams);

                    return checkFilters(filters);
                },
            }),
        },
    },
});
