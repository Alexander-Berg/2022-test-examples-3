import {makeSuite, makeCase} from 'ginny';
import {waitForSuccessfulSnippetListUpdate} from '@self/platform/spec/hermione/helpers/filters';
import SnippetList from '@self/root/src/widgets/content/search/Serp/components/Page/__pageObject';
import FilterList from '@self/platform/spec/page-objects/FilterList';
import Preloadable from '@self/platform/spec/page-objects/preloadable';
import PaymentType from '@self/platform/spec/page-objects/n-payment-type-hint.js';
import {paymentTypesFilterValues} from '@self/platform/spec/hermione/fixtures/filters/paymentTypes';

/**
 * Тесты на взаимодействие фильтров по способам оплаты.
 * @property {PageObject.PaymentType} this.params.paymentType - Способы оплаты
 */
export default makeSuite('Взаимодействие с элементами на странице.', {
    environment: 'kadavr',
    story: {
        beforeEach() {
            this.setPageObjects({
                filterList: () => this.createPageObject(FilterList),
                paymentType: () => this.createPageObject(PaymentType),
                snippetList: () => this.createPageObject(SnippetList),
                preloadable: () => this.createPageObject(Preloadable),
            });
        },
        'При взаимодействии': {
            'фильтры сохраняются в урле и фильтруется выдача.': makeCase({
                test() {
                    const filterOrderIndex = 0;
                    const filterQueryParamName = 'payments';
                    const filterQueryParamValue = paymentTypesFilterValues[filterOrderIndex].id;
                    const paymentTypeText = paymentTypesFilterValues[filterOrderIndex].value.toLowerCase();

                    const clickOnItemByIndex = () => this.filterList.clickItemByIndex(filterOrderIndex + 1);
                    const waitForInvisiblePreloadable = () => this.preloadable.waitForVisible(5000, true);
                    const checkQueryParam = (queryParam, expectedValue) =>
                        this.browser
                            .yaCheckUrlParams({[queryParam]: expectedValue})
                            .should.eventually.to.be.equal(true, 'Проверка параметра на отсутствие/присутствие');

                    return checkQueryParam(filterQueryParamName, false)
                        .then(() => waitForSuccessfulSnippetListUpdate(
                            this.browser,
                            clickOnItemByIndex,
                            this.snippetList
                        ))
                        .then(() => checkQueryParam(filterQueryParamName, filterQueryParamValue))
                        .then(() => waitForInvisiblePreloadable())
                        .then(() => this.paymentType.getPaymentTypeText())
                        .then(paymentTypeTexts => paymentTypeTexts[filterOrderIndex])
                        .should.eventually.to.include(
                            paymentTypeText,
                            'Оффер содержит правильный текст способа оплаты'
                        );
                },
            }),
        },
    },
});
