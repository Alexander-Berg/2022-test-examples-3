import {makeCase, makeSuite} from 'ginny';

/**
 * @param {PageObject.WishlistControl} wishlistControl
 */
export default makeSuite('Кнопка "В избранное"', {
    story: {
        'По умолчанию': {
            'видна': makeCase({
                async test() {
                    const visible = await this.wishlistControl.isVisible();

                    await this.expect(visible).to.be.equal(true, 'Кнопка "В избранное" видна');
                },
            }),
        },
    },
});
