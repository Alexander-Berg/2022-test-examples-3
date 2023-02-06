import {makeCase, makeSuite} from 'ginny';

const SHOP_INFO_LINK_NAME = 'Информация о продавц';

/**
 * Тесты на блок n-w-shop-info.
 * @param {PageObject.ShopsInfo} shopsInfo
 */
export default makeSuite('Блок с информацией о продавце.', {
    story: {
        'Ссылка о продавце': {
            'по умолчанию': {
                'должна присутствовать на странице': makeCase({
                    id: 'marketfront-2301',
                    issue: 'MARKETVERSTKA-27485',
                    test() {
                        return this.shopsInfo.isLinkExists()
                            .should.eventually
                            .to.be.equal(true, 'Блок со ссылкой на информацию присутвует на странице');
                    },
                }),

                'должна вести на страницу с информацией о магазинах': makeCase({
                    id: 'marketfront-2299',
                    issue: 'MARKETVERSTKA-27483',
                    async test() {
                        const {shopIds} = this.params;

                        const actualUrl = await this.shopsInfo.getLinkHref();
                        const actualIds = actualUrl.query.shopIds.split(',').map(Number);

                        await this.expect(actualIds)
                            .to.have.same.members(shopIds, 'id магазинов совпадают');

                        return this.expect(actualUrl, 'Ссылка ведет на страницу с информацией о магазинах')
                            .to.be.link({
                                pathname: '/shop-info',
                            }, {
                                skipProtocol: true,
                                skipHostname: true,
                            });
                    },
                }),

                [`должна содержать текст "${SHOP_INFO_LINK_NAME}ах(е)"`]: makeCase({
                    id: 'marketfront-2300',
                    issue: 'MARKETVERSTKA-27484',
                    test() {
                        const {shopsInfo} = this;
                        return shopsInfo.getLinkHref()
                            .then(actualUrl => {
                                const shopIdsCount = (actualUrl.query.shopIds.split(',')).length;
                                const supplierIdsCount = actualUrl.query.supplierIds
                                    ? (actualUrl.query.supplierIds.split(',')).length
                                    : 0;
                                const idsCount = shopIdsCount + supplierIdsCount;
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
