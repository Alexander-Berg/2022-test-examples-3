import {makeCase, makeSuite} from 'ginny';
import {SHOP_WITHOUT_NAME_TEXT} from '@self/root/src/entities/shop/constants';

/**
 * Проверка блока автора ответа, если ответ от магазина и магазина не существует
 *
 * @param {PageObject.ShopAuthor} shopAuthor
 * @param {string} params.expectedAuthorNameLinkUrl
 * @param {string} params.expectedAuthorLogoUrl
 */

export default makeSuite('Блок снипета ответа c ответом от магазина. Магазин не существует. Блок автора.', {
    story: {
        'Аватар при отображении': {
            'должен содержать заглушку логотипа': makeCase({
                id: 'm-touch-2928',
                issue: 'MOBMARKET-11979',
                feature: 'Ответ магазина',
                async test() {
                    return this.expect(this.shopAuthor.hasEmptyAvatar()).to.be.equal(true);
                },
            }),
            'не должен содержать ссылку на магазин': makeCase({
                id: 'm-touch-2928',
                issue: 'MOBMARKET-11979',
                feature: 'Ответ магазина',
                async test() {
                    return this.expect(this.shopAuthor.hasAvatarLink()).to.be.equal(false);
                },
            }),
        },
        'Имя автора при отображении': {
            [`должно содержать "${SHOP_WITHOUT_NAME_TEXT}"`]: makeCase({
                id: 'm-touch-2928',
                issue: 'MOBMARKET-11979',
                feature: 'Ответ магазина',
                async test() {
                    return this.expect(this.shopAuthor.getNameText()).to.be.equal(SHOP_WITHOUT_NAME_TEXT);
                },
            }),
            'не должно содержать ссылку на магазин': makeCase({
                id: 'm-touch-2928',
                issue: 'MOBMARKET-11979',
                feature: 'Ответ магазина',
                async test() {
                    return this.expect(this.shopAuthor.hasNameLink()).to.be.equal(false);
                },
            }),
        },
        'Синяя галочка подтверждения при отображении': {
            'отображается': makeCase({
                id: 'm-touch-2828',
                issue: 'MOBMARKET-12436',
                feature: 'Ответ магазина',
                async test() {
                    return this.shopAuthor.isVerifiedBadgeVisible().should.eventually.to.be.equal(true);
                },
            }),
        },
    },
});
