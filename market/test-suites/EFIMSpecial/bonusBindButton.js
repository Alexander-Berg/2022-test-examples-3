import {makeCase, makeSuite} from 'ginny';

import {Button} from '@self/root/src/uikit/components/Button/__pageObject';
import EFIMSuccessBindBonusPopup from '@self/root/src/widgets/parts/EFIMSuccessBindBonusPopup/components/View/__pageObject';
import Link from '@self/root/src/components/Link/__pageObject';
import BonusLink from '@self/root/src/components/CoinPopup/BonusLink/__pageObject';

import {bindBonus, TARGET_STEP} from '@self/project/src/spec/hermione/scenarios/EFIMSpecial';

module.exports = makeSuite('Кнопка привязки купона в попапе.', {
    issue: 'BLUEMARKET-10249',
    params: {
        isAuth: 'Авторизован ли пользователь',
        bindButtonText: 'Текст на кнопке привязки купонов',
    },
    story: {
        beforeEach() {
            this.setPageObjects({
                efimSuccessBindBonusPopup: () => this.createPageObject(EFIMSuccessBindBonusPopup),
                efimButtonLinkToBonusCategory: () => this.createPageObject(Button, {
                    root: BonusLink.root,
                }),
                efimLinkToBonusPage: () => this.createPageObject(Link, {
                    parent: EFIMSuccessBindBonusPopup.underBonusBlock,
                }),
            });

            return this.browser.yaScenario(this, bindBonus, TARGET_STEP.FUTURE_COIN_POPUP);
        },
        'Текст кнопки привязки купона должен содержать ожидаемый текст': makeCase({
            async test() {
                await this.popupBindButton.getButtonText()
                    .should.eventually.to.be.equal(
                        this.params.bindButtonText,
                        `Текст кнопки должен содержать ${this.params.bindButtonText}`
                    );
            },
        }),
    },
});
