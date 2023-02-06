import url from 'url';
import {makeCase, makeSuite} from 'ginny';

/**
 * @param {PageObject.Breadcrumbs} breadcrumbs
 */
export default makeSuite('Хлебные крошки (проверка вендора).', {
    environment: 'kadavr',
    story: {
        'При клике на крошку с вендором': {
            'происходит переход на выдачу с проставленным фильтром вендора.': makeCase({
                id: 'marketfront-2569',
                issue: 'MARKETVERSTKA-29266',
                async test() {
                    const linksCount = await this.breadcrumbs.getItemsCount();

                    const vendorHrefLink = await this.breadcrumbs.getItemLinkByIndex(linksCount);
                    const {glfilter} = url.parse(vendorHrefLink, true).query;

                    const [vendorFilterId, vendorFilterValue] = glfilter.split(':');

                    await this.breadcrumbs.clickItemByIndex(linksCount);
                    await this.browser.yaWaitForPageReady();

                    await this.browser.allure.runStep(`Проверяем, что фильтр ${vendorFilterId} виден`, () =>
                        this.browser
                            .isVisible(`[data-autotest-id="${vendorFilterId}"]`)
                            .should.eventually.to.be.equal(true, 'Фильтр виден на странице')
                    );

                    await this.browser.allure.runStep(
                        `Проверяем, что фильтр ${vendorFilterId} со значение ${vendorFilterValue} выбран`,
                        () => this.browser
                            .isSelected(`[id="${vendorFilterId}_${vendorFilterValue}"]`)
                            .should.eventually.to.be.equal(true, 'Фильтр выбран')
                    );
                },
            }),
        },
    },
});
