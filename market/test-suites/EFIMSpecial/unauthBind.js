import {makeCase, makeSuite, prepareSuite, mergeSuites} from 'ginny';

import checkAuthRetpath from '@self/root/src/spec/utils/checkAuthRetpath';
import {bindBonus, TARGET_STEP} from '@self/project/src/spec/hermione/scenarios/EFIMSpecial';

import bonusBindButton from './bonusBindButton';


const BIND_BUTTON_TEXT = 'Войти и забрать';

module.exports = makeSuite('Неавторизованный пользователь', {
    defaultParams: {
        isAuth: false,
    },
    story: mergeSuites(
        // Проверка кнопки привязки бонуса в попапе
        prepareSuite(bonusBindButton, {
            params: {
                isAuth: false,
                bindButtonText: BIND_BUTTON_TEXT,
            },
            meta: {
                id: 'bluemarket-3368',
            },
        }),
        {
            'Открытие попапа с купоном.': {
                beforeEach() {
                    return this.browser.yaScenario(this, bindBonus, TARGET_STEP.FUTURE_COIN_POPUP);
                },
                'Нажатие на кнопку "Войти и забрать" происходит переход на авторизацию с корректным retpath': makeCase({
                    id: 'bluemarket-3369',
                    issue: 'BLUEMARKET-10249',
                    async test() {
                        await this.browser.yaWaitForChangeUrl(() => this.popupBindButton.click());

                        return checkAuthRetpath.call(this, {
                            page: 'market:special',
                            params: {
                                semanticId: 'bonusy',
                                bonusPromoSource: 'source',
                                forceBind: 1,
                            },
                        });
                    },
                }),
            },
        }
    ),
});
