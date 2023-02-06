import {makeCase, makeSuite} from 'ginny';

import {PAGE_IDS_COMMON} from '@self/root/src/constants/pageIds';

/**
 * Тесты на голосование за вопрос в списке вопросов
 * @property {PageObject.Votes} this.questionVotes
 */
export default makeSuite('Голосование за вопрос в списке вопросов. Если пользователь не авторизован', {
    story: {
        'По умолчанию': {
            'При нажатии на лайк происходит редирект в паспорт': makeCase({
                id: 'marketfront-3341',
                issue: 'MARKETVERSTKA-32994',
                async test() {
                    await this.questionVotes.clickLike();

                    const expectedUrl = await this.browser.yaBuildURL(PAGE_IDS_COMMON.LOGIN);
                    // external урлы на паспорт без протокола и парсится как pathname
                    const expectedFullUrl = `https:${expectedUrl}`;
                    const url = await this.browser.yaParseUrl();

                    return this.expect(url).to.be.link(expectedFullUrl, {
                        skipProtocol: true,
                        skipHostname: true,
                    });
                },
            }),
        },
    },
});
