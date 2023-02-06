import {makeCase, makeSuite} from 'ginny';

const SCROLLED_PIXELS_TO_END_PAGE = 3000;

/**
 * Тест на ЧПУ блока n-carousel.
 * @param {PageObject.SnippetCell} snippetCell
 */
export default makeSuite('Сниппет в карусели.', {
    params: {
        title: 'заголовок карусели',
    },
    story: {
        'Ссылка на категорию.': {
            'По умолчанию': {
                'содержит slug и является ЧПУ': makeCase({
                    id: 'marketfront-3025',
                    issue: 'MARKETVERSTKA-31865',
                    async test() {
                        this.browser.scroll(undefined, 0, SCROLLED_PIXELS_TO_END_PAGE);
                        const url = await this.snippetCell.getCategoryUrl();

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
