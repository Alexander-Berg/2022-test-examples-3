import {
    mergeSuites,
    makeSuite,
    makeCase,
} from 'ginny';


import {region} from '@self/root/src/spec/hermione/configs/geo';
import {
    OUTLET_ID,
    openOutletPage,
    checkAddToFavoritesButtonText,
    checkActionBlockVisibility,
} from './helpers';

export default makeSuite('Авторизованный пользователь', {
    defaultParams: {
        isAuth: true,
    },
    story: mergeSuites(
        makeSuite('ПВЗ сохранён в избранных', {
            story: {
                async beforeEach() {
                    await this.browser.setState('persAddress.pickpoint', {
                        [OUTLET_ID]: {
                            regionId: region['Москва'],
                            pickId: OUTLET_ID,
                            lastOrderTime: (new Date()).toISOString(),
                        },
                    });

                    return openOutletPage.call(this);
                },

                'Кнопка добавления ПВЗ в избранные.': {
                    'Отображается c корректным текстом': makeCase({
                        id: 'bluemarket-3940',
                        async test() {
                            this.skip();

                            await checkActionBlockVisibility.call(this);

                            return checkAddToFavoritesButtonText.call(this, 'В адресах');
                        },
                    }),
                },
            },
        }),

        makeSuite('ПВЗ не сохранён в избранных', {
            story: {
                beforeEach() {
                    return openOutletPage.call(this);
                },

                'Кнопка добавления ПВЗ в избранные.': {
                    beforeEach() {
                        return checkActionBlockVisibility.call(this);
                    },

                    'Отображается c корректным текстом': makeCase({
                        id: 'bluemarket-3939',
                        test() {
                            return checkAddToFavoritesButtonText.call(this, 'В мои адреса');
                        },
                    }),

                    'При клике происходит добавление ПВЗ в избранные': makeCase({
                        id: 'bluemarket-3939',
                        async test() {
                            this.skip();

                            await this.browser.allure.runStep(
                                'Кликаем по кнопке добавления в избранное и прооверяем запрос в бэкенд', () => (
                                    this.browser.yaWaitKadavrLogByBackendMethod(
                                        'PersAddress',
                                        'addFavoritePickPoint',
                                        () => this.primaryButton.click()
                                    )
                                        .then(({request}) => request.body.pickId)
                                        .should.eventually.to.be.equal(
                                            Number(OUTLET_ID),
                                            'В запросе на сохранение ПВЗ должен быть корректный id ПВЗ'
                                        )
                                )
                            );

                            return checkAddToFavoritesButtonText.call(this, 'В адресах');
                        },
                    }),
                },
            },
        })
    ),
});
