import {makeSuite, makeCase} from 'ginny';

/**
 * Тесты на блок FeedSnippet
 * @param {PageObject.FeedSnippet} feedSnippet
 */
export default makeSuite('Сниппет виджета Roll.', {
    params: {
        id: 'Идентификатор сущности',
        entity: 'Сущность сниппета (модель / оффер)',
    },
    story: {
        'По умолчанию': {
            'является ссылкой на КО/КМ': makeCase({
                issue: 'MOBMARKET-10920',
                id: 'm-touch-2527',
                async test() {
                    const href = await this.feedSnippet.getHref();

                    const pattern = this.params.entity === 'offer'
                        ? `/offer/${this.params.id}`
                        : `/product/${this.params.id}`;

                    return this.expect(href).to.contain(pattern, 'Ссылка корректная');
                },
            }),

            'имеет необходимые элементы': makeCase({
                issue: 'MOBMARKET-10917',
                id: 'm-touch-2524',
                async test() {
                    const isThumbnailExisting = await this.feedSnippet.hasThumbnail();

                    await this.expect(isThumbnailExisting).to.be.equal(
                        true,
                        'Изображение присутствует'
                    );

                    const isPriceExisting = await this.feedSnippet.hasPrice();

                    await this.expect(isPriceExisting).to.be.equal(
                        true,
                        'Цена присутствует'
                    );

                    const isStickerExisting = await this.feedSnippet.hasDealSticker();

                    return this.expect(isStickerExisting).to.be.equal(
                        false,
                        'Стикер акции Промокод отсутствует'
                    );
                },
            }),
        },
    },
});
