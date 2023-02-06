import url from 'url';
import {pathOrUnsafe, isEqual} from 'ambar';
import {makeSuite, makeCase} from 'ginny';

const SHOP_INFO_LINK_NAME = 'Информация о продавцах';

/**
 * Тесты на ссылку "Информация о продавцах" виджета CreditDisclaimer.
 * @property {PageObject.CreditDisclaimer} creditDisclaimer
 * @property {PageObject.ProductPage} productPage
 * @param {Array<string>} params.shopIds
 */
export default makeSuite('Блок с информацией о продавце.', {
    feature: 'Кредиты на Маркете',
    meta: {
        id: 'm-touch-3328',
        issue: 'MARKETFRONT-11097',
    },
    params: {
        shopIds: 'Массив тех магазинов, товары которых присутствуют на странице.',
    },
    story: {
        'Ссылка о продавце': {
            'по умолчанию': {
                'должна присутствовать на странице.': makeCase({
                    test() {
                        return this.creditDisclaimer.isLinkExists()
                            .should.eventually.to.be.equal(true,
                                'Проверяем, ссылка "информация о продавцах" присутствует.');
                    },
                }),

                'должна вести на страницу с информацией о магазине из ДО': makeCase({
                    test() {
                        const {creditDisclaimer} = this;

                        const {shopIds} = this.params;

                        return creditDisclaimer.getLinkHref()
                            .then(actualUrl => {
                                const actualIds = pathOrUnsafe(
                                    '',
                                    ['query', 'shopIds'],
                                    url.parse(actualUrl, true)
                                ).split(',');

                                const diff = isEqual(actualIds, shopIds);

                                return this.expect(diff, 'id магазинов должны совпадать')
                                    .to.be.equal(true, 'id магазинов совпадают')
                                    .then(() => this
                                        .expect(actualUrl, 'Ссылка ведет на страницу с информацией о магазинах')
                                        .to.be.link({
                                            pathname: '/shops-jur-info',
                                        }, {
                                            skipProtocol: true,
                                            skipHostname: true,
                                        })
                                    );
                            });
                    },
                }),

                [`должна содержать текст "${SHOP_INFO_LINK_NAME}"`]: makeCase({
                    test() {
                        const {creditDisclaimer} = this;
                        return creditDisclaimer.getLinkText()
                            .should.eventually.equal(
                                SHOP_INFO_LINK_NAME,
                                `Название ссылки совпадает с "${SHOP_INFO_LINK_NAME}"`
                            );
                    },
                }),
            },
        },
    },
});
