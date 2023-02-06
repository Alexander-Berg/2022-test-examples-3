import {makeSuite, makeCase} from 'ginny';

/**
 * Тест на компонент Filters
 * @property {PageObject.Filters} filters - компонент списка фильтров
 * @property {PageObject.FilterTumbler} filterTumbler - переключатель фильтра
 */

export default makeSuite('Кнопка "Посмотреть предложения".', {
    story: {
        'При переключении булевого фильтра и нажатии на кнопку': {
            'происходит переход в каталог с включенным фильтром': makeCase({
                issue: 'MOBMARKET-9302',
                id: 'm-touch-1364',
                feature: 'Выдача фильтры',
                async test() {
                    await this.filterTumbler.root.click();

                    await this.browser.allure.runStep(
                        'Ждём совершения запроса к Репорту',
                        // eslint-disable-next-line market/ginny/no-pause
                        () => this.browser.pause(1500)
                    );

                    const changedUrl = await this.browser.yaWaitForChangeUrl(() => this.filters.apply());

                    const expectedUrl = await this.browser.yaBuildURL(
                        'touch:list',
                        Object.assign({glfilter: `${this.params.filterId}:1`}, this.params.route)
                    );

                    return this.expect(changedUrl).to.be.link(expectedUrl, {
                        skipProtocol: true,
                        skipHostname: true,
                    });
                },
            }),
        },
    },
});
