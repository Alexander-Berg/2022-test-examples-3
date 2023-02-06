import {makeCase, makeSuite} from 'ginny';

/**
 * Тест на блок n-brand-headline.
 * @param {PageObject.BrandHeadline} brandHeadline
 */

export default makeSuite('Шапка бренда.', {
    feature: 'Шапка бренда',
    story: {
        'Логотип бренда.': {
            'По-умолчанию': {
                'корректно отображается': makeCase({
                    id: 'marketfront-898',
                    test() {
                        return this.browser
                            .yaSafeAction(this.brandHeadline.waitForLogoVisibility(), false)
                            .should.eventually.be.equal(true, 'Лого отобразилось');
                    },
                }),
            },
        },

        'Кнопка "Все бренды".': {
            'По-умолчанию': {
                'содержит ссылку на /brands': makeCase({
                    id: 'marketfront-896',
                    test() {
                        return this.brandHeadline
                            .waitForAllBrandsVisibility()
                            .then(() => this.brandHeadline.getAllBrandsLink())
                            .should.eventually.be.link(
                                {
                                    pathname: '/brands',
                                },
                                {
                                    skipProtocol: true,
                                    skipHostname: true,
                                });
                    },
                }),
            },
        },

        'Кнопка "О бренде".': {
            'По-умолчанию': {
                'корректно отображается': makeCase({
                    id: 'marketfront-897',
                    test() {
                        return this.browser
                            .yaSafeAction(this.brandHeadline.waitForAboutVisibility(), false)
                            .should.eventually.be.equal(true, 'Кнопка отобразилась');
                    },
                }),
            },
        },
    },
});
