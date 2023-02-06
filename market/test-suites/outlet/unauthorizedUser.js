import {
    mergeSuites,
    makeSuite,
    makeCase,
} from 'ginny';
import url from 'url';
import querystring from 'query-string';

import {
    openOutletPage,
    checkAddToFavoritesButtonText,
    checkActionBlockVisibility,
} from './helpers';

export default makeSuite('Неавторизованный пользователь', {
    defaultParams: {
        isAuth: false,
    },
    story: mergeSuites(
        makeSuite('ПВЗ не сохранён в избранных', {
            story: {
                beforeEach() {
                    return openOutletPage.call(this);
                },

                'Кнопка добаления ПВЗ в избранные.': {
                    beforeEach() {
                        return checkActionBlockVisibility.call(this);
                    },

                    'Отображается c корректным текстом': makeCase({
                        id: 'bluemarket-3938',
                        test() {
                            return checkAddToFavoritesButtonText.call(this, 'В мои адреса');
                        },
                    }),

                    'При клике': {
                        'происходит переход на страницу авторизации и сохранение ПВЗ в избранных': makeCase({
                            id: 'bluemarket-3938',
                            async test() {
                                const passportUrl = await this.browser.yaWaitForChangeUrl(() =>
                                    this.primaryButton.click()
                                );

                                await this.allure.runStep(
                                    'Проверяем, что произошёл редирект на страницу авторизации в Паспорте',
                                    () => (
                                        this.expect(passportUrl).to.be.link({
                                            hostname: 'passport-rc.yandex.ru',
                                            pathname: '/auth',
                                        }, {
                                            mode: 'match',
                                            skipProtocol: true,
                                        })
                                    )
                                );

                                const parsedUrl = url.parse(passportUrl);
                                const query = querystring.parse(parsedUrl.query);
                                const retPath = query.retpath;

                                await this.browser.yaMdaTestLogin(undefined, undefined, retPath);

                                await checkActionBlockVisibility.call(this);

                                return checkAddToFavoritesButtonText.call(this, 'В адресах');
                            },
                        }),
                    },
                },
            },
        })
    ),
});
