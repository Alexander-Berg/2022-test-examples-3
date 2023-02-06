import {makeCase, mergeSuites, makeSuite} from 'ginny';

/**
 * Тест на блок n-geo-snippet в контексте выбора оффера
 * (после нажатия на ссылку "Ещё N магазинов") - тут имеются ввиду аутлеты магазина
 * @param {PageObject.GeoSnippet} geoSnippet
 */
export default makeSuite('Сниппет.', {
    story: {
        'По умолчанию': mergeSuites({
            beforeEach() {
                return this.geoSnippet.waitForVisible();
            },

            'должен содержать тип аутлета': makeCase({
                async test() {
                    await this.geoSnippet.checkOutletTypeVisibility(false);
                },
            }),

            'должен содержать адрес аутлета': makeCase({
                async test() {
                    await this.geoSnippet.checkForShopAddressVisibility(false);
                },
            }),

            'должен содержать график работы аутлета': makeCase({
                async test() {
                    await this.geoSnippet.checkForWorkingTimeVisibility(false);
                },
            }),

            'должен содержать телефон аутлета': makeCase({
                async test() {
                    await this.geoSnippet.checkForPhoneNumberVisibility(false);
                },
            }),

            'должен содержать условия доставки': makeCase({
                async test() {
                    await this.geoSnippet.checkForPickupVisibility(false);
                },
            }),

            'кнопку "В магазин"': makeCase({
                id: 'marketfront-1146',
                issue: 'MARKETVERSTKA-25334',
                async test() {
                    await this.geoSnippet.checkForActionButtonVisibility(false);
                },
            }),
        }),
    },
});
