import {makeCase, makeSuite, mergeSuites} from 'ginny';
import Votes from '@self/platform/spec/page-objects/components/Votes';

/**
 * @param {PageObject.AnswerSnippet} answerSnippet
 * @param {PageObject.Votes} votes
 */
export default makeSuite('Блок голосовалки когда пользователь не авторизован.', {
    story: mergeSuites(
        {
            async beforeEach() {
                this.setPageObjects({
                    votes: () => this.createPageObject(
                        Votes,
                        {
                            parent: this.answerSnippet,
                        }
                    ),
                });
            },
        },
        {
            'Когда кликаем лайк': {
                'происходит редирект в паспорт': makeCase({
                    id: 'm-touch-2261',
                    issue: 'MOBMARKET-9116',
                    feature: 'Лайки/дизлайки',
                    async test() {
                        await this.votes.clickLike();

                        const changedUrl = this.browser.getUrl();
                        return this.expect(changedUrl)
                            .to.be.link({
                                hostname: 'passport-rc.yandex.ru',
                                pathname: '/auth',
                            }, {
                                skipProtocol: true,
                            });
                    },
                }),
            },
            'Когда кликаем дизлайк': {
                'проиcходит редирект в паспорт': makeCase({
                    id: 'm-touch-2311',
                    issue: 'MOBMARKET-9272',
                    feature: 'Лайки/дизлайки',
                    async test() {
                        await this.votes.clickDislike();

                        const changedUrl = this.browser.getUrl();
                        return this.expect(changedUrl)
                            .to.be.link({
                                hostname: 'passport-rc.yandex.ru',
                                pathname: '/auth',
                            }, {
                                skipProtocol: true,
                            });
                    },
                }),
            },
        }
    )}
);
