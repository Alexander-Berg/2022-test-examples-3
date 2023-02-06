import {makeCase, makeSuite} from 'ginny';

/**
 * Проверка даты создания в блоке автора магазина
 * @param {PageObject.ShopAuthor} shopAuthor
 * @param {string} params.expectedCreatedAtText
 */
export default makeSuite('Блок автора магазина. Дата создания.', {
    story: {
        'По умолчанию': {
            'отображается': makeCase({
                id: 'm-touch-2831',
                issue: 'MOBMARKET-12439',
                feature: 'Ответ магазина',
                async test() {
                    return this.expect(this.shopAuthor.getCreatedAtText())
                        .to.be.match(this.params.expectedCreatedAtText);
                },
            }),
        },
    },
});
