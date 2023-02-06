import {makeCase, makeSuite, mergeSuites} from 'ginny';

import checkAuthRetpath from '@self/root/src/spec/utils/checkAuthRetpath';
import {Button} from '@self/root/src/uikit/components/Button/__pageObject';

module.exports = makeSuite('Привязка купона.', {
    id: 'bluemarket-2798',
    issue: 'BLUEMARKET-9351',
    story: mergeSuites(
        {
            beforeEach() {
                this.setPageObjects({
                    bonusButton: () => this.createPageObject(Button, {parent: this.bindBonus}),
                });
            },
        },

        makeSuite('По кнопке.', {
            story: {
                'При клике на кнопку "Получить купон"': {
                    beforeEach() {
                        return this.browser.yaWaitForChangeUrl(() => this.bonusButton.click());
                    },

                    'происходит переход на авторизацию с корректным retpath': makeCase({
                        test() {
                            return checkAuthRetpath.call(this, {
                                page: 'market:bonus-bind',
                                params: {
                                    token: this.params.promo.id,
                                },
                            });
                        },
                    }),
                },
            },
        }),

        makeSuite('Автоматически.', {
            defaultParams: {
                isAutoBind: true,
                isPassportPageFirst: true,
            },
            story: {
                'Происходит переход на авторизацию с корректным retpath': makeCase({
                    test() {
                        return checkAuthRetpath.call(this, {
                            page: 'market:bonus-bind',
                            params: {
                                token: this.params.promo.id,
                            },
                        });
                    },
                }),
            },
        })
    ),
});
