import {makeSuite, makeCase} from 'ginny';

/**
 * Тесты на ЧПУ блока snippet-card.
 * @param {PageObject.SnippetCard} snippetCard
 * @param {PageObject.SnippetCell} snippetCell
 */
export default makeSuite('Сниппет КМ', {
    environment: 'kadavr',
    story: {
        'Ссылка на категорию.': {
            'По умолчанию': {
                'содержит slug и является ЧПУ': makeCase({
                    id: 'marketfront-3025',
                    issue: 'MARKETVERSTKA-31865',
                    async test() {
                        const isCardView = await this.snippetCard.isVisible();
                        const url = isCardView
                            ? await this.snippetCard.getCategoryUrl()
                            : await this.snippetCell.getCategoryUrl();

                        return this.expect(url).to.be.link({
                            pathname: '/catalog--.*/\\d+',
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
