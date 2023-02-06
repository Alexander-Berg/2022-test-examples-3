import {makeCase, makeSuite, mergeSuites} from 'ginny';

import Text from '@self/root/src/uikit/components/Text/__pageObject';
import Link from '@self/root/src/components/Link/__pageObject';
import EFIMAdditionalInfo from '@self/root/src/components/EFIMAdditionalInfo/__pageObject';

const DEFAULT_TEXT = 'Количество товаров и купонов ограничено.';
const LINK_TEXT = 'Подробнее об условиях акции';

module.exports = makeSuite('Блок с условиями акции.', {
    id: 'bluemarket-3380',
    issue: 'BLUEMARKET-6631',
    story: mergeSuites(
        {
            beforeEach() {
                this.setPageObjects({
                    efimAdditionalInfo: () =>
                        this.createPageObject(EFIMAdditionalInfo),
                    additionalText: () =>
                        this.createPageObject(Text, {
                            parent: this.efimAdditionalInfo,
                        }),
                    additionalLink: () =>
                        this.createPageObject(Link, {
                            parent: this.efimAdditionalInfo,
                        }),
                });
            },
        },
        makeSuite('По умолчанию', {
            story: {
                'Блок с условиями акции отображается и содержит текст с ссылкой': makeCase({
                    async test() {
                        await this.efimAdditionalInfo.isExisting()
                            .should.eventually.be.equal(
                                true,
                                'Блок с условиями акции должен отображаться'
                            );
                        await this.additionalText.getText()
                            .should.eventually.be.equal(
                                DEFAULT_TEXT,
                                `Текст c условиями акции должен содержать "${DEFAULT_TEXT}"`
                            );
                        await this.additionalLink.getText()
                            .should.eventually.to.be.equal(
                                LINK_TEXT,
                                `Текст ссылки должен содержать "${LINK_TEXT}"`
                            );
                    },
                }),
                'Ссылка ведет на страницу с правилами акции': makeCase({
                    async test() {
                        const tabIds = await this.browser.getTabIds();
                        await this.additionalLink.click();
                        const newTabId = await this.browser.yaWaitForNewTab({startTabIds: tabIds});

                        await this.allure.runStep(
                            'Переключаемся на новую вкладку и проверяем URL',
                            async () => {
                                await this.browser.switchTab(newTabId);

                                await this.browser.getUrl()
                                    .should.eventually.to.be.link({
                                        pathname: '/legal/coupon/',
                                        hostname: 'yandex.ru',
                                    }, {
                                        mode: 'match',
                                        skipProtocol: true,
                                    });

                                // закрыть новую вкладку
                                await this.browser.close();
                            }
                        );
                    },
                }),
            },
        })
    ),
});
