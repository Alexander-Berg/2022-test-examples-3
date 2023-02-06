import {makeCase, makeSuite} from 'ginny';

/**
 * Тест на кликабельные элементы блока n-breadcrumbs.
 * @property {PageObject.Breadcrumbs} breadcrumbs
 * @property {PageObject.Headline} headline
 */

export default makeSuite('Кликабельные хлебные крошки.', {
    feature: 'SEO',
    environment: 'testing',
    story: {
        'По умолчанию': {
            'должны иметь заголовки, совпадающие с названиями.': makeCase({
                id: 'marketfront-2117',
                issue: 'MARKETVERSTKA-27115',
                test() {
                    return this.breadcrumbs.itemLinks
                        .then(({value: links}) =>
                            // INFO: используем reduce только для лучшей читаемости сгенерированного allure-отчета
                            links.reduce((chainPromise, item, index) => {
                                const itemOrderIndex = index + 1;

                                return chainPromise
                                    .then(() => this.breadcrumbs.getItemTextByIndex(itemOrderIndex)
                                        .then(text => Promise.all([
                                            text,
                                            this.breadcrumbs.getItemTitleByIndex(itemOrderIndex),
                                        ]))
                                        .then(([text, title]) => this.expect(title)
                                            .to.be.equal(
                                                text,
                                                `Тайтл крошки с индексом "${itemOrderIndex}" ` +
                                                `совпадает с названием "${text}"`)
                                        ));
                            }, Promise.resolve())
                        );
                },
            }),
        },
        'При клике на каждую крошку': {
            'открывается страница с указанным заголовком.': makeCase({
                id: 'marketfront-2560',
                issue: 'MARKETVERSTKA-27115',
                params: {
                    categoryName: 'Название категории',
                },
                test() {
                    return this.breadcrumbs.itemLinks
                        .then(({value: links}) => this.browser.allure.runStep(
                            `Прокликиваем все кликабельные крошки: ${links.length} шт.`,
                            () => Promise.resolve(links.map(
                                async (link, index, linksCount) => {
                                    const linkIndex = index + 1;

                                    return this.breadcrumbs.getItemTextByIndex(linkIndex)
                                        .then(text => Promise.all([
                                            text,
                                            this.browser.yaWaitForPageReloaded(
                                                () => this.breadcrumbs.clickItemByIndex(linkIndex)
                                            ),
                                        ]))
                                        .then(([itemText]) => {
                                            const isLastItem = linkIndex === linksCount;
                                            const {categoryName, type} = this.params;
                                            let expectedItemText = itemText;
                                            let headline;

                                            if (type === 'modification') {
                                                if (isLastItem) {
                                                    expectedItemText = itemText;
                                                } else if (itemText !== categoryName) {
                                                    expectedItemText = `${categoryName} ${itemText}`;
                                                }
                                            } else if (type === 'cluster' && isLastItem) {
                                                expectedItemText = categoryName;
                                            } else if (isLastItem && categoryName) {
                                                expectedItemText = `${categoryName} ${itemText}`;
                                            }

                                            // INFO: КМ сверстана иначе, для нее нужен свой PageObject
                                            if (type === 'modification' && isLastItem) {
                                                headline = this.productSummary;
                                            } else {
                                                headline = this.headline;
                                            }

                                            return headline.getHeaderTitleText().should.eventually.be.equal(
                                                expectedItemText,
                                                `Title страницы должен совпадать со строкой "${expectedItemText}"`
                                            );
                                        })
                                        .then(() => (linkIndex < linksCount ? this.browser.yaHistoryBack() : null));
                                })))
                        );
                },
            }),
        },
    },
});
