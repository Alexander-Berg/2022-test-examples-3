import {PAGE_IDS_COMMON} from '@self/root/src/constants/pageIds';
import {
    getIsMaxReturnTimeLimitReachedOnly,
    isNonReturnableTimeReason as getIsNonReturnableTimeReason,
} from '@self/root/src/entities/returnableItem/getters';
import {getUnavailableReturnReasonContent} from '@self/root/src/entities/returnableItem/utils';
import {NON_BREAKING_SPACE_CHAR as NBSP} from '@self/root/src/constants/string';


const {makeCase, makeSuite} = require('ginny');

module.exports = makeSuite('Возврат товара', {
    params: {
        nonReturnableReasons: 'Причины, по которым недоступен возврат товара',
    },
    story: {
        async beforeEach() {
            await this.allure.runStep('Отображается кнопка "Вернуть заказ"', async () => {
                const isExist = await this.orderCard.isExisting(this.orderCard.returnButton);
                return this.expect(isExist).to.be.equal(true, 'Кнопка "Вернуть заказ" должна отображаться');
            });

            await this.allure.runStep('Кнопка "Вернуть заказ" по клику открывает страницу возврата',
                async () => {
                    await this.browser.yaWaitForChangeUrl(() => this.orderCard.clickReturnButton(), 20000);

                    return this.browser.getUrl()
                        .should.eventually.to.be
                        .link({
                            pathname: '/my/returns/create',
                            query: {
                                orderId: String(this.params.orderId),
                                type: 'refund',
                            },
                        }, {
                            skipHostname: true,
                            skipProtocol: true,
                        });
                });

            await this.allure.runStep('Отображается ссылка на условия возврата', () =>
                this.unavailableText.isReturnDetailsLinkVisible()
                    .should.eventually.be
                    .equal(true, 'Ссылка "Подробнее про условия возврата" должна быть видимой'));

            await this.allure.runStep('Ссылка "Подробнее про условия возврата" должна иметь корректный href', async () => {
                const builtUrl = await this.browser.yaBuildURL(PAGE_IDS_COMMON.YANDEX_SUPPORT_RETURNS);

                const EXPECTED_HREF = `https:${builtUrl}`;

                return this.unavailableText.getReturnDetailsLinkHref()
                    .should.eventually.be
                    .equal(EXPECTED_HREF, `Ссылка "Подробнее про условия возврата" должна вести на ${EXPECTED_HREF}`);
            });
        },

        'Корректно отображается содержимое экрана о возврате.': makeCase({
            async test() {
                const {nonReturnableReasons} = this.params;
                const isNonReturnableTimeReason = getIsNonReturnableTimeReason(nonReturnableReasons);
                const isMaxReturnTimeLimitReachedOnly = getIsMaxReturnTimeLimitReachedOnly(nonReturnableReasons);

                if (isNonReturnableTimeReason || isMaxReturnTimeLimitReachedOnly) {
                    return;
                }

                await this.allure.runStep('Отображается правильное описание причины невозвратности', async () => {
                    const EXPECTED_DESCRIPTION_TEXT = getExpectedDescriptionText(nonReturnableReasons);

                    return this.expect(await this.unavailableText.descriptionText.getText()).to.be
                        .equal(EXPECTED_DESCRIPTION_TEXT, `Описание должно быть "${EXPECTED_DESCRIPTION_TEXT}"`);
                });
            },
        }),

        'Корректно отображается содержимое экрана о возврате невозвратного товара.': makeCase({
            async test() {
                const {nonReturnableReasons} = this.params;
                const isNonReturnableTimeReason = getIsNonReturnableTimeReason(nonReturnableReasons);
                const isMaxReturnTimeLimitReachedOnly = getIsMaxReturnTimeLimitReachedOnly(nonReturnableReasons);

                if (isNonReturnableTimeReason && !isMaxReturnTimeLimitReachedOnly) {
                    await this.allure.runStep('Отображается текст про сервисный центр', async () => {
                        const EXPECTED_SERVICE_TEXT = 'Если товар неисправен или является технически сложным, ' +
                            'обратитесь в рекомендованный производителем сервисный центр.';

                        return this.expect(await this.unavailableText.serviceCentersText.getText()).to.be
                            .equal(EXPECTED_SERVICE_TEXT, `Текст должен быть "${EXPECTED_SERVICE_TEXT}"`);
                    });

                    await this.allure.runStep('Отображается ссылка на список сервисных центров', () =>
                        this.unavailableText.isServiceCentersLinkVisible()
                            .should.eventually.be
                            .equal(true, 'Ссылка "Список сервисных центров" должна быть видимой'));

                    await this.allure.runStep('Ссылка "Список сервисных центров" должна иметь корректный href', async () => {
                        const builtUrl = await this.browser.yaBuildURL(PAGE_IDS_COMMON.YANDEX_SUPPORT_SERVICE);

                        const EXPECTED_HREF = `https:${builtUrl}`;

                        return this.unavailableText.getServiceCentersLinkHref()
                            .should.eventually.be
                            .equal(EXPECTED_HREF, `Ссылка "Список сервисных центров" должна вести на ${EXPECTED_HREF}`);
                    });
                }
            },
        }),

        'Корректно отображается содержимое экрана возврата товара, полученного более 15 дней назад': makeCase({
            async test() {
                const {nonReturnableReasons} = this.params;
                const isMaxReturnTimeLimitReachedOnly = getIsMaxReturnTimeLimitReachedOnly(nonReturnableReasons);

                if (isMaxReturnTimeLimitReachedOnly) {
                    await this.allure.runStep('Отображается правильный заголовок при возврате товара, полученного более 15 дней назад', async () => {
                        const EXPECTED_TITLE = 'Увы, вернуть товары уже не получится';

                        return this.expect(await this.maxReturnTimeReached.lateReturnTitle.getText()).to.be
                            .equal(EXPECTED_TITLE, `Заголовок должен быть "${EXPECTED_TITLE}"`);
                    });

                    await this.allure.runStep('Отображается правильное описание при возврате товара, полученного более 15 дней назад', async () => {
                        const EXPECTED_LATE_RETURN_TEXT = 'Вы получили заказ больше 15 дней назад. Если какие-то товары неисправны, ' +
                            'мы рекомендуем обратиться в авторизованный сервисный центр.';

                        return this.expect(await this.maxReturnTimeReached.lateReturnText.getText()).to.be
                            .equal(EXPECTED_LATE_RETURN_TEXT, `Описание должно быть "${EXPECTED_LATE_RETURN_TEXT}"`);
                    });

                    await this.allure.runStep('Отображается ссылка на список сервисных центров', () =>
                        this.maxReturnTimeReached.isServiceCenterLinkVisible()
                            .should.eventually.be
                            .equal(true, 'Ссылка "сервисный центр" должна быть видимой'));

                    await this.allure.runStep('Ссылка "сервисный центр" должна иметь корректный href', async () => {
                        const builtUrl = await this.browser.yaBuildURL(PAGE_IDS_COMMON.YANDEX_SUPPORT_SERVICE);

                        const EXPECTED_HREF = `https:${builtUrl}`;

                        return this.maxReturnTimeReached.getServiceCenterLinkHref()
                            .should.eventually.be
                            .equal(EXPECTED_HREF, `Ссылка "Список сервисных центров" должна вести на ${EXPECTED_HREF}`);
                    });
                }
            },
        }),
    },
});

const getExpectedDescriptionText = reasons => {
    const content = getUnavailableReturnReasonContent(reasons);

    return content && content.text
        .replace(new RegExp(NBSP, 'g'), ' ')
        // Вырезаем HTML-теги
        .replace(/<(.|\n)*?>/g, '');
};
