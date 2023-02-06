import {makeSuite, makeCase} from 'ginny';
import {getLastReportRequestParams} from '@self/platform/spec/hermione/helpers/getBackendRequestParams';
import {FILTER_ID, FILTER_VALUES, FILTER_LABELS} from '@self/platform/spec/hermione/fixtures/credit';

/**
 * Тесты на взаимодействие фильтров "Покупка в кредит".
 * @param {PageObject.FilterRadio} filterList
 * @param {PageObject.FilterCounter} filterCounter Плашка с количеством найденных результатов
 */
export default makeSuite('Работа фильтра "Покупка в кредит".', {
    environment: 'kadavr',
    feature: 'Кредиты на Маркете',
    params: {
        place: 'Плейс репорта, в который уходят запросы',
    },
    story: {
        [`При выборе значения «${FILTER_LABELS.CREDIT}»`]: {
            'сответствующий параметр передаётся в Репорт': makeCase({
                async test() {
                    await this.filterList.clickLabel(FILTER_ID, FILTER_VALUES.CREDIT);
                    await this.filterCounter.waitForPopupIsVisible();

                    const {[FILTER_ID]: creditType} = await getLastReportRequestParams(this, this.params.place);

                    return this.expect(creditType).to.be.equal(
                        FILTER_VALUES.CREDIT,
                        `В запросе к Репорту присутствует параметр "credit-type = ${FILTER_VALUES.CREDIT}"`
                    );
                },
            }),
            'в урле появляется сответствующий параметр': makeCase({
                async test() {
                    await this.filterList.clickLabel(FILTER_ID, FILTER_VALUES.CREDIT);
                    await this.filterCounter.waitForPopupIsVisible();

                    return this.browser
                        .yaCheckUrlParams({[FILTER_ID]: FILTER_VALUES.CREDIT})
                        .should.eventually.to.be.equal(true,
                            `В урле появился параметр "credit-type = ${FILTER_VALUES.CREDIT}"`
                        );
                },
            }),
        },
    },
});
