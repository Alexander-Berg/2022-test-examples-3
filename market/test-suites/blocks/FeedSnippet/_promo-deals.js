import {makeSuite, makeCase} from 'ginny';

/**
 * Тесты на блок FeedSnippet
 * @param {PageObject.FeedSnippet} feedSnippet
 */
export default makeSuite('Сниппет виджета фид.', {
    story: {
        'По умолчанию': {
            'является ссылкой на КО': makeCase({
                issue: 'MARKETVERSTKA-31586',
                id: 'marketfront-2936',
                async test() {
                    const href = await this.feedSnippet.getHref();

                    return this.expect(href).to.contain(
                        '/offer/',
                        'Ссылка корректная'
                    );
                },
            }),

            'имеет необходимые элементы': makeCase({
                issue: 'MARKETVERSTKA-31510',
                id: 'marketfront-2925',
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

                    const isBadgeExisting = await this.feedSnippet.hasDealsBadge();

                    return this.expect(isBadgeExisting).to.be.equal(
                        true,
                        'Бейдж акции присутствует'
                    );
                },
            }),
        },
    },
});
