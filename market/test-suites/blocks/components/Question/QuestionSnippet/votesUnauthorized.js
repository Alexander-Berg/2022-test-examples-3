import {makeCase, makeSuite} from 'ginny';

import {PAGE_IDS_COMMON} from '@self/root/src/constants/pageIds';

/**
 * Тесты на лайк вопроса, если пользователь не авторизован
 * @property {PageObject.Votes} this.questionVotes
 */
export default makeSuite('Лайк вопроса. Если пользователь не авторизован', {
    environment: 'kadavr',
    story: {
        'При клике на лайк': {
            'происходит редирект на паспорт': makeCase({
                id: 'marketfront-2897',
                issue: 'MARKETVERSTKA-31281',
                feature: 'Лайки/дизлайки',
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
