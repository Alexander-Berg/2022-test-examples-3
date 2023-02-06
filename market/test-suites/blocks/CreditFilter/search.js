import {makeSuite, makeCase} from 'ginny';
import {differenceWith, compose, isEmpty, isEqual} from 'ambar';
import {getLastReportRequestParams} from '@self/platform/spec/hermione/helpers/getLastReportRequestParams';
import {FILTER_VALUES, FILTER_LABELS, FILTER_NO_MATTER_LABEL, FILTER_ID} from '@self/platform/spec/hermione/fixtures/credit';

/**
 * @property {PageObject.FilterCompound} filterCompound
 * @property {PageObject.Filters} filters
 * @property {PageObject.SearchProduct} snippet
 * @property {PageObject.SelectFilter} selectFilter
 * @property {PageObject.FilterPopup} filterPopup
 */
export default makeSuite('Фильтр по покупке в кредит на выдаче.', {
    feature: 'Кредиты на Маркете',
    story: {
        'По умолчанию': {
            'присутствует на странице': makeCase({
                id: 'm-touch-2926',
                issue: 'MOBMARKET-12880',
                async test() {
                    await this.filterCompound.clickOnCard();

                    const textOptionPromises = Object.values(FILTER_VALUES).map(filterValue =>
                        this.selectFilter.getValueItemById(filterValue).getText());

                    const expectedTextOptions = Object.values(FILTER_LABELS);

                    const expectedOptionsExist = Promise.all(textOptionPromises).then(
                        compose(isEmpty, differenceWith(isEqual, expectedTextOptions))
                    );

                    return this.expect(expectedOptionsExist).to.be.equal(true, 'Фильтр присутствует на странице');
                },
            }),

            [`выбрано значение «${FILTER_NO_MATTER_LABEL}»`]: makeCase({
                id: 'm-touch-2927',
                issue: 'MOBMARKET-12879',
                async test() {
                    await this.filterCompound.clickOnCard();
                    const selectedOptionText = await this.selectFilter.getSelectedOptionText();

                    return this.expect(selectedOptionText).to.be.equal(
                        FILTER_NO_MATTER_LABEL,
                        `выбрано значение «${FILTER_NO_MATTER_LABEL}»`
                    );
                },
            }),
        },
        [`При выборе значения «${FILTER_VALUES.CREDIT}»`]: {
            'сответствующий параметр передаётся в Репорт': makeCase({
                id: 'm-touch-2924',
                issue: 'MOBMARKET-12881',
                async test() {
                    await this.filterCompound.clickOnCard();

                    await this.selectFilter.waitForVisible();

                    await this.selectFilter.selectValueItemById(FILTER_VALUES.CREDIT);

                    await this.filterPopup.apply();

                    await this.filters.waitForApplyButtonActive();

                    const {[FILTER_ID]: creditType} = await getLastReportRequestParams(this, 'prime');

                    return this.expect(creditType).to.be.equal(
                        FILTER_VALUES.CREDIT,
                        `В запросе к Репорту присутствует параметр "credit-type = ${FILTER_VALUES.CREDIT}"`
                    );
                },
            }),
        },
        [`При выборе значения «${FILTER_VALUES.INSTALLMENT}»`]: {
            'сответствующий параметр передаётся в Репорт': makeCase({
                id: 'm-touch-2925',
                issue: 'MOBMARKET-12883',
                async test() {
                    await this.filterCompound.clickOnCard();

                    await this.selectFilter.waitForVisible();

                    await this.selectFilter.selectValueItemById(FILTER_VALUES.INSTALLMENT);

                    await this.filterPopup.apply();

                    await this.filters.waitForApplyButtonActive();

                    const {[FILTER_ID]: creditType} = await getLastReportRequestParams(this, 'prime');

                    return this.expect(creditType).to.be.equal(
                        FILTER_VALUES.INSTALLMENT,
                        `В запросе к Репорту присутствует параметр "${FILTER_ID} = ${FILTER_VALUES.INSTALLMENT}"`
                    );
                },
            }),
        },
        [`При выборе значения «${FILTER_VALUES.INSTALLMENT}»`]: {
            [`на карточку модели прокидывается параметр "${FILTER_ID}=${FILTER_VALUES.INSTALLMENT}"`]:
                makeCase({
                    id: 'm-touch-2819',
                    issue: 'MOBMARKET-12823',
                    async test() {
                        await this.filterCompound.clickOnCard();

                        await this.selectFilter.waitForVisible();

                        await this.selectFilter.selectValueItemById(FILTER_VALUES.INSTALLMENT);

                        await this.filterPopup.apply();

                        await this.filters.waitForApplyButtonActive();

                        await this.filters.apply();

                        await this.snippet.click();

                        return this.browser.getUrl().should.eventually.be.link({
                            query: {
                                [FILTER_ID]: FILTER_VALUES.INSTALLMENT,
                            },
                        }, {
                            mode: 'match',
                            skipProtocol: true,
                            skipHostname: true,
                        });
                    },
                }),
        },
    },
});
