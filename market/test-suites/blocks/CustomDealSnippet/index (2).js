import {makeSuite, makeCase} from 'ginny';

/**
 * Тесты на блок CustomDealSnippet
 * @property {PageObject.CustomDealSnippet} customDealSnippet
 */
export default makeSuite('Сниппет кастомной акции.', {
    params: {
        expectedLink: 'Ожидаемая ссылка',
        expectedTitle: 'Ожидаемый заголовок',
    },
    story: {
        'По умолчанию': {
            'является ссылкой': makeCase({
                issue: 'MOBMARKET-13217',
                id: 'm-touch-2970',
                async test() {
                    const href = await this.customDealSnippet.getHref();

                    return this.expect(href).to.be.link({
                        path: this.params.expectedLink,
                    }, {
                        mode: 'match',
                        skipProtocol: true,
                        skipHostname: true,
                    });
                },
            }),
            'отображает заголовок': makeCase({
                issue: 'MOBMARKET-13217',
                id: 'm-touch-2970',
                async test() {
                    const title = await this.customDealSnippet.getTitle();

                    return this.expect(title).to.equal(this.params.expectedTitle, 'Заголовок корректен');
                },
            }),
        },
    },
});
