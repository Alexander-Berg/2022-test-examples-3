import {makeSuite, makeCase} from 'ginny';

/**
 * Тесты ЧПУ ссылку "Все товары бренда"
 * @property {PageObject.AllVendorProductsLink} allVendorProductLink
 */

export default makeSuite('Визитка карточки модели', {
    feature: 'Визитка',
    story: {
        'Ссылка "Все товары бренда"': {
            'по умолчанию': {
                'содержит slug и является ЧПУ': makeCase({
                    id: 'marketfront-3067',
                    issue: 'MARKETVERSTKA-32871',
                    async test() {
                        const url = await this.allVendorProductLink.getBrandUrl();

                        return this.expect(url).to.be.link({
                            pathname: 'brands--[\\w-]+/\\d+',
                        }, {
                            mode: 'match',
                            skipProtocol: true,
                            skipHostname: true,
                        });
                    },
                }),
            },
        },
        'Ссылка на логотипе бренда': {
            'по умолчанию': {
                'содержит slug и является ЧПУ': makeCase({
                    id: 'marketfront-3067',
                    issue: 'MARKETVERSTKA-32871',
                    async test() {
                        const url = await this.allVendorProductLink.getBrandLogoUrl();

                        return this.expect(url).to.be.link({
                            pathname: 'brands--[\\w-]+/\\d+',
                        }, {
                            mode: 'match',
                            skipProtocol: true,
                            skipHostname: true,
                        });
                    },
                }),
            },
        },
    },
});
