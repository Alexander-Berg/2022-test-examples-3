import {makeSuite, makeCase} from 'ginny';

/**
 * Тест на ссылку «Ещё/Скрыть» блока FilterList.
 * @param {PageObject.FilterList} filterList
 */
export default makeSuite('Списковой фильтр. Кнопка «Ещё/Скрыть».', {
    feature: 'Фильтр типа «ENUM»',
    environment: 'kadavr',
    params: {
        checkboxQuantities: 'Кол-во чекбоксов, которые видно до нажатия кнопки «Ещё»',
    },
    story: {
        'При клике': {
            'отображаются/скрываются доп. варианты, меняется название': makeCase({
                id: 'marketfront-718',
                issue: 'MARKETVERSTKA-24055',
                async test() {
                    const {checkboxQuantities} = this.params;

                    const clickOnFooterLink = () => this.filterList.clickOnFooterLink();
                    const compareControlsQuantities = (compareFunctionName = 'equal', compareText = 'равно') =>
                        this.browser.allure.runStep('Сравниваем кол-во видимых чекбоксов', () =>
                            this.filterList
                                .controls
                                .then(({value}) => value.length)
                                .then(quantities => this.expect(quantities)
                                    .to.be[compareFunctionName](
                                        checkboxQuantities,
                                        `Кол-во чекбоксов ${compareText} ${checkboxQuantities}`
                                    ))
                        );
                    const checkLinkName = expectedText =>
                        this.browser.allure.runStep('Проверяем название ссылки', () =>
                            this.filterList
                                .footerLink.getText()
                                .then(linkText => this.expect(linkText).to.be.include(
                                    expectedText,
                                    `Название ссылки содержит «${expectedText}»`
                                ))
                        );

                    await checkLinkName('Показать ещё');
                    await compareControlsQuantities();
                    await clickOnFooterLink();
                    await checkLinkName('Свернуть');
                    await compareControlsQuantities('greaterThan', 'больше, чем');
                    await clickOnFooterLink();
                    await checkLinkName('Показать ещё');
                    await compareControlsQuantities();
                },
            }),
        },
    },
});
