import {makeCase, makeSuite} from 'ginny';

/**
 * @param {PageObject.Breadcrumbs} breadcrumbs
 * @param {PageObject.Headline|PageObject.ShopHeadline} headline
 */
export default makeSuite('Хлебные крошки (совпадение с заголовком).', {
    environment: 'kadavr',
    story: {
        'Последняя крошка': {
            'По условию': {
                'совпадает с заголовком.': makeCase({
                    id: 'marketfront-2111',
                    issue: 'MARKETVERSTKA-30236',
                    async test() {
                        const lastIndex = await this.breadcrumbs.getItemsCount();
                        const lastItemText = await this.breadcrumbs.getItemTextByIndex(lastIndex);
                        const headerText = await this.headline.getHeaderTitleText();

                        return this.browser.allure.runStep(
                            'Проверяем что текст последней крошки совпадает с заголовком',
                            () => lastItemText.should.be.equal(
                                headerText,
                                'Текст последней хлебной крошки совпадает с заголовком'
                            )
                        );
                    },
                }),
            },
        },
    },
});
