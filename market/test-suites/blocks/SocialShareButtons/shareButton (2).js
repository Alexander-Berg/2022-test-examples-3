import {makeSuite, makeCase} from 'ginny';

const urlBuildersMapping = {
    vk: ({url}) => `https://vk.com/share.php?url=${url}`,
    ok: ({url}) => `https://connect.ok.ru/offer?url=${url}`,
    fb: ({url}) => `https://www.facebook.com/sharer/sharer.php?u=${url}`,
    whatsapp: ({url}) => `https://api.whatsapp.com/send?text=${url}`,
    telegram: ({url}) => `https://t.me/share/url?url=${url}`,
    moimir: ({url}) => `http://connect.mail.ru/share?url=${url}`,
    twitter: ({url}) => `https://twitter.com/intent/tweet?url=${url}`,
};

const buildShareUrl = (service, shareInfo) => {
    const urlBuilder = urlBuildersMapping[service];

    return urlBuilder({
        ...shareInfo,
        url: encodeURIComponent(shareInfo.url),
    });
};

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
