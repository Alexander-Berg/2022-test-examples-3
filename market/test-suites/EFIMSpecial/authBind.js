import {makeSuite, mergeSuites, prepareSuite} from 'ginny';

import {Button} from '@self/root/src/uikit/components/Button/__pageObject';
import EFIMSuccessBindBonusPopup from '@self/root/src/widgets/parts/EFIMSuccessBindBonusPopup/components/View/__pageObject';
import Link from '@self/root/src/components/Link/__pageObject';
import BonusLink from '@self/root/src/components/CoinPopup/BonusLink/__pageObject';
import {bindBonus, TARGET_STEP} from '@self/project/src/spec/hermione/scenarios/EFIMSpecial';

import bonusBindButton from './bonusBindButton';
import additionalInfo from './additionalInfo';
import sucessBondedPopup from './successBindedPopup';

const BIND_BUTTON_TEXT = 'Забрать купон';

module.exports = makeSuite('Авторизованный пользователь.', {
    defaultParams: {
        isAuth: true,
    },
    story: mergeSuites(
        // Проверка кнопки привязки бонуса в попапе
        prepareSuite(bonusBindButton, {
            params: {
                isAuth: true,
                bindButtonText: BIND_BUTTON_TEXT,
            },
            meta: {
                id: 'bluemarket-3372',
            },
        }),
        // Попап с успешной привязкой бонуса
        prepareSuite(sucessBondedPopup, {
            meta: {
                id: 'bluemarket-3373',
            },
            hooks: {
                async beforeEach() {
                    /**
                     * В тест сьюте sucessBindedPopup данные pageObject'ы будут созданы,
                     * но сценарий требует проверки наличия этих pageObject'ов
                     */
                    this.setPageObjects({
                        efimSuccessBindBonusPopup: () => this.createPageObject(EFIMSuccessBindBonusPopup),
                        efimButtonLinkToBonusCategory: () => this.createPageObject(Button, {
                            root: BonusLink.root,
                        }),
                        efimLinkToBonusPage: () => this.createPageObject(Link, {
                            parent: EFIMSuccessBindBonusPopup.underBonusBlock,
                        }),
                    });

                    await this.browser.yaScenario(this, bindBonus, TARGET_STEP.BIND_BONUS_SUCCESS);
                },
            },
        }),
        // блок с правилами акции
        prepareSuite(additionalInfo)
    ),
});
