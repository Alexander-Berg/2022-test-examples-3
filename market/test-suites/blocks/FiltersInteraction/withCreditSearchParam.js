
import {makeSuite, makeCase} from 'ginny';
import {FILTER_ID, FILTER_VALUES} from '@self/platform/spec/hermione/fixtures/credit';

/**
 * Тесты на взаимодействие фильтров по способам оплаты.
 * @param {PageObject.FilterRadio} filterList - Список опций фильтра
 * @param {PageObject.FilterCounter} filterCounter - Плашка с количеством найденных результатов
 * @param {PageObject.SnippetCell2} snippet - Cниппет товара
 */
export default makeSuite('Работа фильтра "Покупка в кредит". Переход на КМ.', {
    environment: 'kadavr',
    story: {

        [`При выборе значения «${FILTER_VALUES.CREDIT}»`]: {
            [`на карточку модели прокидывается параметр "${FILTER_ID}=${FILTER_VALUES.CREDIT}"`]:
                makeCase({
                    async test() {
                        await this.filterList.clickLabel(FILTER_ID, FILTER_VALUES.CREDIT);

                        await this.filterCounter.waitForPopupIsVisible();

                        await this.browser.allure.runStep(
                            'Скроллим до блока "Ещё предложения"',
                            () => this.snippet
                                .getSelector(this.snippet.morePricesLink)
                                .then(selector => this.browser.scroll(selector, 0, -200))
                        );

                        return this.browser.allure.runStep(
                            `На карточку модели прокидывается параметр "${FILTER_ID}=${FILTER_VALUES.CREDIT}"`,
                            () => this.snippet.getMorePricesHref().should.eventually.be.link({
                                query: {
                                    [FILTER_ID]: FILTER_VALUES.CREDIT,
                                },
                            }, {
                                mode: 'match',
                                skipProtocol: true,
                                skipHostname: true,
                            })
                        );
                    },
                }),
        },

    },
});
