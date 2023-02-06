import {makeCase, makeSuite} from 'ginny';
import {createProduct} from '@yandex-market/kadavr/mocks/Report/helpers';

/**
 * @param {PageObject.Breadcrumbs} breadcrumbs
 * @param {PageObject.Headline|PageObject.ShopHeadline} headline
 * @param {PageObject.FilterList} filter
 */
export default makeSuite('Хлебные крошки (сброс некликабельных).', {
    environment: 'kadavr',
    story: {
        'Последняя кликабельная крошка': {
            'При клике': {
                'переходим на выдачу, пропадает некликабельная, заголовок изменяется, фильтры сбрасываются.': makeCase({
                    id: 'marketfront-2153',
                    issue: 'MARKETVERSTKA-30237',
                    async test() {
                        const hasLink = await this.breadcrumbs.hasLastItemLink();

                        this.browser.allure.runStep(
                            'Проверяем что последняя хлебная крошка некликабельная',
                            () => hasLink.should.be.equal(
                                true,
                                'Последняя хлебная крошка некликабельна'
                            )
                        );

                        const oldLastIndex = await this.breadcrumbs.getItemsCount();
                        const oldLastItemText = await this.breadcrumbs.getItemTextByIndex(oldLastIndex);
                        // Сбрасываем стейт, чтоб не оставались зажатые фильтры или рецепты
                        await this.browser.setState('report', createProduct({slug: 'product'}, '1'));
                        await this.breadcrumbs.clickItemByIndex(oldLastIndex - 1);
                        await this.browser.yaWaitForPageReady();

                        const newLastIndex = await this.breadcrumbs.getItemsCount();
                        const newLastItemText = await this.breadcrumbs.getItemTextByIndex(newLastIndex);

                        this.browser.allure.runStep(
                            'Проверяем что последняя хлебная крошка исчезла',
                            () => {
                                oldLastIndex.should.be.equal(
                                    newLastIndex + 1,
                                    'Кол-во хлебных крошек уменьшилось на 1'
                                );

                                oldLastItemText.should.not.be.equal(
                                    newLastItemText,
                                    'Последняя хлебная крошка отличается от предыдущей'
                                );
                            }
                        );

                        const headerText = await this.headline.getHeaderTitleText();
                        this.browser.allure.runStep(
                            'Проверяем что текст последней крошки совпадает с заголовком',
                            () => newLastItemText.should.be.equal(
                                headerText,
                                'Текст последней хлебной крошки совпадает с заголовком'
                            )
                        );

                        const {selector} = await this.filter.root;
                        const isFilterExisting = await this.browser.isExisting(selector);

                        this.browser.allure.runStep(
                            'Проверяем что фильтр сброшен',
                            () => isFilterExisting.should.be.equal(
                                false,
                                'Фильтр должен быть сброшенным'
                            )
                        );
                    },
                }),
            },
        },
    },
});
