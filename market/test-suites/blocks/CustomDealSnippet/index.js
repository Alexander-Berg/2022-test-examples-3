import url from 'url';
import {makeSuite, makeCase} from 'ginny';

/**
 * Тесты на блок CustomDealSnippet
 * @param {PageObject.CustomDealSnippet} customDealSnippet
 */
export default makeSuite('Сниппет кастомной акции.', {
    params: {
        expectedLink: 'Ожидаемая ссылка',
        expectedTitle: 'Ожидаемый заголовок',
    },
    story: {
        'По умолчанию': {
            'является ссылкой': makeCase({
                issue: 'MARKETVERSTKA-34507',
                id: 'marketfront-2515',
                async test() {
                    const href = await this.customDealSnippet.getHref();
                    const {path} = url.parse(href);

                    return this.expect(path).to.equal(this.params.expectedLink, 'Ссылка корректна');
                },
            }),
            'отображает заголовок': makeCase({
                issue: 'MARKETVERSTKA-34507',
                id: 'marketfront-2515',
                async test() {
                    const title = await this.customDealSnippet.getTitle();

                    return this.expect(title).to.equal(this.params.expectedTitle, 'Заголовок корректен');
                },
            }),
        },
    },
});
