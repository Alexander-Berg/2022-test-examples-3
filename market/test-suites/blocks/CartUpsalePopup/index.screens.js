import {makeCase, makeSuite} from '@yandex-market/ginny';

export default makeSuite('Попап корзины (upsale)', {
    environment: 'kadavr',
    feature: 'cartPopup',
    story: {
        'По-умолчанию': {
            'Отображается корректно': makeCase({
                async test() {
                    await this.searchSnippet.waitForVisible();
                    await this.cartButton.click();
                    const popupContentSelector = await this.popupContent.getSelector();

                    await this.cartPopup.waitForVisible();
                    return this.browser.assertView('popupContent', popupContentSelector, {
                        compositeImage: true,
                    });
                },
            }),
        },
    },
});
