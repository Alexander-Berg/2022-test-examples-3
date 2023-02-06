import {makeSuite, makeCase} from 'ginny';

/**
 * @property {PageObject.VisualFilter} visualFilter
 * @property {PageObject.FilterPopup} filterPopup
 * @property {PageObject.SelectFilter} selectFilter
 */
export default makeSuite('Фильтр цвета', {
    params: {
        filterId: 'Идентификатор фильтра',
        applyFilterValueId: 'Применяемое значение фильтра',
    },

    story: {
        'По умолчанию': {
            'отображается на странице': makeCase({
                id: 'm-touch-2891',
                issue: 'MOBMARKET-12701',
                test() {
                    return this.browser.allure.runStep('Проверям видимость фильтра', () =>
                        this.visualFilter.isVisible()
                            .should.eventually.to.be.equal(true, 'Блок отображается')
                    );
                },
            }),
        },
        'При клике по выбранному цвету': {
            'значение цвета применяется': makeCase({
                id: 'm-touch-2892',
                issue: 'MOBMARKET-12701',
                async test() {
                    await this.visualFilter.waitForVisible();

                    await this.visualFilter.clickOnValueById(this.params.applyFilterValueId);

                    const value = await this.visualFilter.getCurrentActiveValues();

                    await this.expect(value.length).to.be.equal(1, 'Выбрано 1 активное значение фильтра');

                    return this.expect(Number(value[0])).to.be.equal(
                        this.params.applyFilterValueId,
                        `Значение применённого цвета должно быть ${this.params.applyFilterValueId}`
                    );
                },
            }),
        },
        'При клике по крестику закрытия': {
            'фильтр выбора цвета возвращается в исходное состояние': makeCase({
                id: 'm-touch-2893',
                issue: 'MOBMARKET-12701',
                async test() {
                    await this.visualFilter.waitForVisible();

                    await this.visualFilter.clickOnValueById(this.params.applyFilterValueId);

                    const values = await this.visualFilter.getCurrentActiveValues();

                    await this.expect(values.length).to.be.equal(1, 'Выбрано 1 активное значение фильтра');

                    await this.visualFilter.clickOnResetControl();

                    await this.visualFilter.waitForResetControlNotExists();

                    const finalValues = await this.visualFilter.getCurrentActiveValues();

                    return this.expect(finalValues.length).to.be.equal(0, 'Нет активных выбранных значений фильтра');
                },
            }),
        },
        'При клике по кнопке +N вариантов': {
            'осуществляет переход к фильтру выбора цвета': makeCase({
                id: 'm-touch-2895',
                issue: 'MOBMARKET-12701',
                async test() {
                    await this.visualFilter.waitForVisible();

                    await this.visualFilter.clickOnShowAllValuesControl();

                    return this.browser.allure.runStep(
                        'Проверям видимость попапа со всеми значениями фильтра',
                        () => this.filterPopup.isVisible()
                            .should.eventually.to.be.equal(true, 'Попап отображается')
                    );
                },
            }),
            'и применении фильтров': {
                'осуществляется переход на КМ с выбранным фильтром по цвету': makeCase({
                    id: 'm-touch-2896',
                    issue: 'MOBMARKET-12701',

                    async test() {
                        await this.visualFilter.waitForVisible();
                        await this.visualFilter.clickOnShowAllValuesControl();

                        const valueIndex = 2;
                        await this.selectFilter.waitForVisible();
                        const expectedFilterVal = await this.selectFilter.getZonedValueIdByIndex(valueIndex);
                        await this.browser.allure.runStep(
                            `Применяем ${valueIndex}-оe значение фильтра`,
                            () => this.selectFilter.getZonedItemByIndex(valueIndex).click()
                        );

                        await this.filterPopup.apply();
                        await this.visualFilter.waitForVisible();

                        return this.browser.yaParseUrl()
                            .should.eventually.be.link({
                                query: {
                                    glfilter: `${this.params.filterId}:${expectedFilterVal}`,
                                },
                            }, {
                                mode: 'match',
                                skipProtocol: true,
                                skipHostname: true,
                                skipPathname: true,
                            });
                    },
                }),
            },
        },
    },
});
