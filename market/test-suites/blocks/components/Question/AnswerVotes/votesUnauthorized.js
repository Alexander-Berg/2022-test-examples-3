import {makeCase, makeSuite} from 'ginny';

import {PAGE_IDS_COMMON} from '@self/root/src/constants/pageIds';

/**
 * Тесты на голосование за ответ
 * @property {PageObject.Votes} this.answerVotes
 */
export default makeSuite('Голосование за ответ. Если пользователь не авторизован', {
    story: {
        'По умолчанию': {
            'При нажатии на лайк происходит редирект в паспорт': makeCase({
                id: 'marketfront-2898',
                issue: 'MARKETVERSTKA-31283',
                async test() {
                    await this.answerVotes.clickLike();

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

            'При нажатии на дизлайк происходит редирект в паспорт': makeCase({
                id: 'marketfront-2899',
                issue: 'MARKETVERSTKA-31284',
                async test() {
                    await this.answerVotes.clickDislike();

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
