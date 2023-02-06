import {makeCase, makeSuite} from 'ginny';

import {Button} from '@self/root/src/uikit/components/Button/__pageObject';
import EFIMSuccessBindBonusPopup from '@self/root/src/widgets/parts/EFIMSuccessBindBonusPopup/components/View/__pageObject';
import Link from '@self/root/src/components/Link/__pageObject';
import BonusLink from '@self/root/src/components/CoinPopup/BonusLink/__pageObject';

module.exports = makeSuite('Попап с успешно привязанным купоном.', {
    defaultParams: {
        isAuth: true,
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
        },
        'Кнопка "Выбрать товары" должна отображаться и иметь ссылку на категорию': makeCase({
            id: 'bluemarket-3374',
            issue: 'BLUEMARKET-10249',
            async test() {
                await this.efimSuccessBindBonusPopup.waitForPopupIsVisible();

                return this.browser.yaWaitForChangeUrl(() => this.browser.allure.runStep(
                    'Кликаем по кнопке "Выбрать товары"',
                    () => this.efimButtonLinkToBonusCategory.click()
                ))
                    .should.eventually.to.be.link({
                        pathname: '/search',
                    }, {
                        mode: 'match',
                        skipProtocol: true,
                        skipHostname: true,
                    });
            },
        }),
        'Ссылка на страницу "Купоны" отрабатывает корректно': makeCase({
            id: 'bluemarket-3375',
            issue: 'BLUEMARKET-10249',
            async test() {
                await this.efimSuccessBindBonusPopup.waitForPopupIsVisible();

                await this.browser.yaWaitForChangeUrl(() => this.browser.allure.runStep(
                    'Кликаем по кнопке "Купоны"',
                    () => this.efimLinkToBonusPage.click()
                ))
                    .should.eventually.to.be.link({
                        pathname: '/bonus',
                    }, {
                        mode: 'match',
                        skipProtocol: true,
                        skipHostname: true,
                    });
            },
        }),
    },
});
