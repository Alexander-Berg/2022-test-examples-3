import {makeSuite, makeCase} from 'ginny';

/**
 * Тесты на попап-агитацию оставления отзыва на товар
 * @property {PageObjects.ReviewPollPopup} reviewPollPopup
 * @property {PageObjects.AgitationPollCard} agitationPollCard
 * @property {PageObjects.ExpertiseMotivation} expertiseMotivation
 * @property {PageObjects.VideoAgitationScreen} videoAgitationScreen
 */
export default makeSuite('Агитация снять видео о товаре.', {
    story: {
        'По умолчанию': {
            'должно отображаться правильное название товара.': makeCase({
                id: 'm-touch-3550',
                async test() {
                    await this.reviewPollPopup.waitForOpened();

                    return this.expect(await this.agitationPollCard.getTitleText())
                        .to.be.equal('Дополните отзыв о товаре myProductName', 'Название товара должно быть правильным');
                },
            }),
        },
        'При клике на кнопку "Добавить видео"': {
            'должна выставиться кука "ugcp".': makeCase({
                id: 'm-touch-3552',
                async test() {
                    await this.reviewPollPopup.waitForOpened();
                    await this.videoAgitationScreen.clickAddVideoButton();

                    const cookie = await this.browser.getCookie('ugcp');
                    return this.expect(cookie.value)
                        .to.be.equal('1', 'Значение должно быть 1');
                },
            }),
            'должен произойти переход на страницу загрузки видео о товаре.': makeCase({
                id: 'm-touch-3551',
                async test() {
                    await this.reviewPollPopup.waitForOpened();
                    const currentTabId = await this.browser.getCurrentTabId();

                    await this.videoAgitationScreen.clickAddVideoButton();

                    const newTabId = await this.browser.yaWaitForNewTab({startTabIds: [currentTabId], timeout: 2000});

                    await this.browser.allure.runStep(
                        'Переключаем вкладку на только что открытую вкладку добавления видео',
                        () => this.browser.switchTab(newTabId)
                    );

                    const currentUrl = await this.browser.getUrl();
                    const expectedUrl = await this.browser.yaBuildURL('market:product-video-add', {
                        productId: this.params.productId,
                        slug: this.params.slug,
                    });

                    await this.browser.allure.runStep(
                        'Проверяем URL открытой страницы',
                        () => this.expect(currentUrl).to.be.link(
                            expectedUrl,
                            {
                                skipProtocol: true,
                                skipHostname: true,
                            }
                        )
                    );
                },
            }),
        },
        'Бейдж с баллами экспертизы': {
            'должен отображаться.': makeCase({
                id: 'm-touch-3549',
                async test() {
                    await this.reviewPollPopup.waitForOpened();
                    await this.expect(this.expertiseMotivation.isVisible())
                        .to.be.equal(true, 'Бейдж должен быть виден');
                },
            }),
        },
    },
});
