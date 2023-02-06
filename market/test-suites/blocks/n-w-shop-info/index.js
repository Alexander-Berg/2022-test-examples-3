import _ from 'lodash';
import {makeCase, makeSuite} from 'ginny';

const SHOP_INFO_LINK_NAME = 'Информация о продавц';

/**
 * Тесты на блок n-w-shop-info.
 * @param {PageObject.ShopsInfo} shopsInfo
 * @param {PageObject.ShopsTop6Info} shopsTop6Info
 * @param {PageObject.ShopName} shopName
 */
export default makeSuite('Блок с информацией о продавце.', {
    feature: 'Карточка модели',
    story: {
        'Ссылка о продавце': {
            'по умолчанию': {
                'должна присутствовать на странице': makeCase({
                    id: 'marketfront-2191',
                    test() {
                        return this.shopsInfo.isLinkExists()
                            .should.eventually
                            .to.be.equal(true, 'Блок со ссылкой на информацию присутвует на странице');
                    },
                }),

                /**
                 * @expFlag dsk_km-do_trust-rev
                 * @ticket MARKETFRONT-71593
                 * @start
                 */
                // В настоящий момент ShopName не используется
                // 'должна вести на страницу с информацией о магазине из ДО': makeCase({
                //     id: 'marketfront-2189',
                //     test() {
                //         const {shopsTop6Info, shopsInfo, shopName} = this;
                //
                //         return Promise.all([
                //             shopsInfo.getLinkHref(),
                //             shopsTop6Info.getShopIds(),
                //             shopName.getShopId(),
                //         ])
                //             .then(([actualUrl, shopIds, shopId]) => {
                //                 const foundShopIds = _.union(shopIds, [shopId]);
                //                 const actualIds = _.get(actualUrl, 'query.shopIds').split(',');
                //                 const diff = _.isEmpty(_.xor(actualIds, foundShopIds));
                //                 return this.expect(diff, 'ид магазинов должны совпадать')
                //                     .to.be.equal(true, 'Ид магазинов совпадают')
                //                     .then(() => this
                //                         .expect(actualUrl, 'Ссылка ведет на страницу с информацией о магазинах')
                //                         .to.be.link({
                //                             pathname: '/shop-info',
                //                         }, {
                //                             skipProtocol: true,
                //                             skipHostname: true,
                //                         })
                //                     );
                //             });
                //     },
                // }),

                [`должна содержать текст "${SHOP_INFO_LINK_NAME}ах(е)"`]: makeCase({
                    id: 'marketfront-2190',
                    test() {
                        // eslint-disable-next-line market/ginny/no-skip
                        return this.skip('MARKETVERSTKA-31797 скипаем упавшие тесты для озеленения');

                        // eslint-disable-next-line no-unreachable
                        const {shopsInfo} = this;
                        return shopsInfo.getLinkHref()
                            .then(actualUrl => {
                                const idsCount = (_.get(actualUrl, 'query.shopIds').split(',')).length;
                                let shopInfoLinkName = SHOP_INFO_LINK_NAME;
                                shopInfoLinkName += (idsCount > 1 ? 'ах' : 'е');

                                return shopsInfo.getLinkText()
                                    .should.eventually.equal(
                                        shopInfoLinkName,
                                        `Название ссылки совпадает с "${shopInfoLinkName}"`
                                    );
                            });
                    },
                }),
            },
        },
    },
});
