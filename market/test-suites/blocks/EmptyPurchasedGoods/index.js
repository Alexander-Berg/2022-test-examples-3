import {makeCase, makeSuite} from 'ginny';

import ZeroStateCard from '@self/project/src/components/ZeroStateCard/__pageObject';

export default makeSuite('Пустая история.', {
    environment: 'kadavr',
    params: {
        pageId: 'Открываемая страница',
    },
    defaultParams: {
        pageId: 'market:purchased',
    },
    story: {
        async beforeEach() {
            this.setPageObjects({
                zeroState: () => this.createPageObject(ZeroStateCard),
            });
        },

        'Неавторизованный пользователь.': {
            async beforeEach() {
                return this.browser.yaOpenPage(this.params.pageId);
            },

            'Клик по кнопке переводит на страницу авторизации': makeCase({
                async test() {
                    await this.zeroState.clickButton();

                    return this.browser.yaParseUrl()
                        .should
                        .eventually
                        .be
                        .link({
                            hostname: 'passport-rc.yandex.ru',
                            pathname: '/auth',
                        }, {
                            skipProtocol: true,
                        });
                },
            }),
        },

        'Авторизованный пользователь.': {
            async beforeEach() {
                return this.browser.yaProfile('pan-topinambur', this.params.pageId);
            },

            'Клик по кнопке переводит на главную страницу': makeCase({
                async test() {
                    await this.zeroState.clickButton();

                    const currentUrl = await this.browser.getUrl();
                    const expectedUrl = await this.browser.yaBuildURL('market:index');

                    return this.expect(currentUrl)
                        .to
                        .be
                        .link(expectedUrl, {
                            skipProtocol: true,
                            skipHostname: true,
                        });
                },
            }),
        },
    },
});
