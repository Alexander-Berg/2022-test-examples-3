import {buildShareUrl} from '@self/project/src/helpers/socialSharing';

import {makeSuite, makeCase} from 'ginny';


/**
 * Тесты на разворачивалку в блоке соц кнопок
 * @property {PageObject.SocialShareButtons} socialShareButtons
 */
export default makeSuite('Кнопка шаринга.', {
    params: {
        index: 'Порядковый номер кнопки',
        service: 'Соцсеть',
        shareInfo: 'Информация для шаринга',
    },
    story: {
        'должна присутствовать и быть работоспособной.': makeCase({
            test() {
                const {shareInfo, service, index} = this.params;
                const href = buildShareUrl(service, shareInfo);

                return this.socialShareButtons
                    .isShareButtonExists(index)
                    .should.eventually.be.equal(true, 'Кнопка присутствует в блоке')
                    .then(() => this.socialShareButtons.getShareButtonHrefByIndex(index))
                    .should.eventually.be.equal(href, 'Url кнопки соответствует ожидаемому')
                    .then(() => this.browser.allure.runStep(
                        'Получаем идентификатор текущей вкладки',
                        () => this.browser.getCurrentTabId()
                    ))
                    .then(currentTabId => this.socialShareButtons
                        .clickShareButtonByIndex(index)
                        .then(() => this.browser.yaWaitForNewTab({startTabIds: [currentTabId], timeout: 2000}))
                        .then(newTabId => this.browser
                            .yaDelay(200)
                            .then(() => this.browser.allure.runStep(
                                'Переключаем вкладку на только что открытую',
                                () => this.browser.switchTab(newTabId)
                            ))
                            .then(() => this.browser.yaDelay(200))
                            .then(() => this.browser.allure.runStep(
                                'Закрываем вкладку',
                                () => this.browser.close()
                            ))
                            .then(() => this.browser.yaDelay(200))
                            .then(() => this.browser.allure.runStep(
                                'Переключаем вкладку на начальную',
                                () => this.browser.switchTab(currentTabId)
                            ))
                            .then(() => this.browser.yaDelay(200))));
            },
        }),
    },
});
