import {makeCase, makeSuite} from 'ginny';

/**
 * Проверка блока автора ответа, если ответ от магазина и магазин существует
 *
 * @param {PageObject.ShopAuthor} shopAuthor
 * @param {string} params.expectedAuthorName
 * @param {string} params.expectedAuthorNameLinkUrl
 * @param {string} params.expectedAuthorLogoUrl
 */
export default makeSuite('Блок снипета ответа c ответом от магазина. Магазин существует. Блок автора.', {
    story: {
        'Аватар при отображении': {
            'должен содержать логотип магазина': makeCase({
                id: 'm-touch-2829',
                issue: 'MOBMARKET-12437',
                feature: 'Ответ магазина',
                async test() {
                    return this.expect(this.shopAuthor.getAvatarImageUrl())
                        .to.be.link(this.params.expectedAuthorLogoUrl, {
                            skipProtocol: true,
                        });
                },
            }),
            'должен содержать ссылку на магазин': makeCase({
                id: 'm-touch-2912',
                issue: 'MOBMARKET-12730',
                feature: 'Ответ магазина',
                async test() {
                    return this.expect(this.shopAuthor.getAvatarLinkUrl())
                        .to.be.link(this.params.expectedAuthorNameLinkUrl, {
                            skipProtocol: true,
                            skipHostname: true,
                        });
                },
            }),
        },
        'Имя автора при отображении': {
            'должно содержать название магазина': makeCase({
                id: 'm-touch-2830',
                issue: 'MOBMARKET-12438',
                feature: 'Ответ магазина',
                async test() {
                    return this.expect(this.shopAuthor.getNameText()).to.be.equal(this.params.expectedAuthorName);
                },
            }),
            'должно содержать ссылку на магазин': makeCase({
                id: 'm-touch-2912',
                issue: 'MOBMARKET-12730',
                feature: 'Ответ магазина',
                async test() {
                    return this.expect(this.shopAuthor.getNameLinkUrl())
                        .to.be.link(this.params.expectedAuthorNameLinkUrl, {
                            skipProtocol: true,
                            skipHostname: true,
                        });
                },
            }),
        },
        'Синяя галочка подтверждения при отображении': {
            'отображается': makeCase({
                id: 'm-touch-2828',
                issie: 'MOBMARKET-12436',
                feature: 'Ответ магазина',
                async test() {
                    return this.expect(this.shopAuthor.isVerifiedBadgeVisible()).to.be.equal(true);
                },
            }),
        },
    },
});
