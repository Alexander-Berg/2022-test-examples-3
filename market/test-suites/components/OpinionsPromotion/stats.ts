'use strict';

import {makeSuite, makeCase} from 'ginny';
import {ContextWithParams} from 'ginny-helpers';

import ProductInfoLabel from 'spec/page-objects/ProductInfoLabel';
import TextLevitan from 'spec/page-objects/TextLevitan';
import OpinionsPromotionAgitationStats from 'spec/page-objects/OpinionsPromotionAgitationStats';

type TestContext = ContextWithParams<{
    agitationStats: OpinionsPromotionAgitationStats;
    modelsCount: ProductInfoLabel;
    charges: ProductInfoLabel;
}>;

/**
 * Тесты на блок статистики по отзывам за баллы
 *
 * @param {PageObject.OpinionsPromotionAgitationStats} agitationStats - блок со статистикой
 */
export default makeSuite('Статистика по отзывам за баллы.', {
    issue: 'VNDFRONT-3993',
    environment: 'kadavr',
    params: {
        user: 'Пользователь',
    },
    story: {
        async beforeEach(this: TestContext) {
            this.setPageObjects({
                spinner(this: TestContext) {
                    return this.createPageObject('SpinnerLevitan', this.agitationStats);
                },
                heading(this: TestContext) {
                    return this.createPageObject('TextLevitan', this.agitationStats.header);
                },
                generateReportButton(this: TestContext) {
                    return this.createPageObject(
                        'ButtonLevitan',
                        this.agitationStats,
                        this.agitationStats.generateReportButton,
                    );
                },
                modelsCount(this: TestContext) {
                    return this.createPageObject(
                        'ProductInfoLabel',
                        this.agitationStats,
                        `${ProductInfoLabel.root}:nth-child(1)`,
                    );
                },
                charges(this: TestContext) {
                    return this.createPageObject(
                        'ProductInfoLabel',
                        this.agitationStats,
                        `${ProductInfoLabel.root}:nth-child(2)`,
                    );
                },
                modelsCountText(this: TestContext) {
                    return this.createPageObject('TextLevitan', this.modelsCount, `${TextLevitan.root}:last-child`);
                },
                chargesText(this: TestContext) {
                    return this.createPageObject('TextLevitan', this.charges, `${TextLevitan.root}:last-child`);
                },
            });

            await this.browser.allure.runStep('Дожидаемся появления блока со статистикой', () =>
                // @ts-expect-error(TS2339) найдено в рамках VNDFRONT-4580
                this.agitationStats.waitForExist(),
            );

            await this.browser.allure.runStep('Дожидаемся загрузки блока со статистикой', () =>
                this.browser.waitUntil(
                    async () => {
                        // @ts-expect-error(TS2339) найдено в рамках VNDFRONT-4580
                        const visible = await this.spinner.isVisible();

                        return visible === false;
                    },
                    this.browser.options.waitforTimeout,
                    'Не удалось дождаться скрытия спиннера',
                ),
            );

            // @ts-expect-error(TS2339) найдено в рамках VNDFRONT-4580
            await this.heading.isVisible().should.eventually.be.equal(true, 'Заголовок блока статистики отображается');

            // @ts-expect-error(TS2339) найдено в рамках VNDFRONT-4580
            await this.heading.getText().should.eventually.be.equal('Статистика', 'Текст заголовка блока корректный');

            // @ts-expect-error(TS2339) найдено в рамках VNDFRONT-4580
            await this.generateReportButton
                .isVisible()
                .should.eventually.be.equal(true, 'Кнопка формирования отчёта отображается');
        },
        'Когда баллы и цели не назначены': {
            'блок статистики': {
                'отображается корректно': makeCase({
                    id: 'vendor_auto-1308',
                    environment: 'kadavr',
                    async test() {
                        this.setPageObjects({
                            messageHeading() {
                                return this.createPageObject(
                                    'TextLevitan',
                                    this.agitationStats.nonPromotedMessage,
                                    `${TextLevitan.root}:nth-child(1)`,
                                );
                            },
                            messageInfo() {
                                return this.createPageObject(
                                    'TextLevitan',
                                    this.agitationStats.nonPromotedMessage,
                                    `${TextLevitan.root}:nth-child(2)`,
                                );
                            },
                        });

                        await this.agitationStats.nonPromotedMessage
                            .isVisible()
                            .should.eventually.be.equal(true, 'Сообщение о ненастроенном сборе баллов отображается');

                        await this.messageHeading
                            .getText()
                            .should.eventually.be.equal(
                                'У вас пока не настроен сбор отзывов за баллы Плюса',
                                'Заголовок сообщения корректный',
                            );

                        await this.messageInfo
                            .getText()
                            .should.eventually.be.equal(
                                'Настройте его в таблице ниже — укажите, сколько баллов будут получать ' +
                                    'покупатели за отзывы и сколько всего отзывов надо собрать.',
                                'Текст сообщения корректный',
                            );
                    },
                }),
            },
        },
        'Когда баллы назначены, а цели не назначены': {
            'блок статистики': {
                'отображается корректно': makeCase({
                    id: 'vendor_auto-1309',
                    environment: 'kadavr',
                    async test() {
                        this.setPageObjects({
                            messageHeading() {
                                return this.createPageObject(
                                    'TextLevitan',
                                    this.agitationStats,
                                    this.agitationStats.noAgitationsMessage,
                                );
                            },
                        });

                        await this.agitationStats.noAgitationsMessage
                            .isVisible()
                            .should.eventually.be.equal(true, 'Сообщение об отсутствии целей отображается');

                        await this.messageHeading
                            .getText()
                            .should.eventually.be.equal(
                                'У вас ещё нет ни одной цели по количеству отзывов',
                                'Заголовок сообщения корректный',
                            );

                        await this.modelsCountText
                            .getText()
                            .should.eventually.be.equal(
                                '3 товаров',
                                'Количество товаров, для которых назначен сбор отображается корректно',
                            );

                        await this.chargesText
                            .getText()
                            .should.eventually.be.equal('0 у. е.0 ₽', 'Ожидаемые расходы отображаются корректно');
                    },
                }),
            },
        },
        'Когда баллы и цели назначены': {
            'блок статистики': {
                'отображается корректно': makeCase({
                    id: 'vendor_auto-1310',
                    environment: 'kadavr',
                    async test() {
                        await this.modelsCountText
                            .getText()
                            .should.eventually.be.equal(
                                '2 товаров',
                                'Количество товаров, для которых назначен сбор отображается корректно',
                            );

                        await this.chargesText
                            .getText()
                            .should.eventually.be.equal(
                                '24 013,44 у. е.720 403,20 ₽',
                                'Ожидаемые расходы отображаются корректно',
                            );

                        await this.agitationStats.progressInfo
                            .getText()
                            .should.eventually.be.equal(
                                '2 из 3\nотзывов собрано для 1 цели',
                                'Прогресс по сбору целей отображается корректно',
                            );
                    },
                }),
            },
        },
        'При формировании отчёта по отзывам за баллы': {
            'генерируется ссылка на скачивание': makeCase({
                id: 'vendor_auto-1340',
                issue: 'VNDFRONT-3998',
                environment: 'testing',
                async test() {
                    this.setPageObjects({
                        loadingButton() {
                            return this.createPageObject(
                                'ButtonLevitan',
                                this.agitationStats,
                                this.agitationStats.loadingButton,
                            );
                        },
                        downloadReportButton() {
                            return this.createPageObject(
                                'Link',
                                this.agitationStats,
                                this.agitationStats.downloadButton,
                            );
                        },
                    });

                    await this.generateReportButton
                        .isVisible()
                        .should.eventually.be.equal(true, 'Кнопка "Сформировать отчёт" отображается');

                    await this.browser.allure.runStep('Нажимаем на кнопку "Сформировать отчёт"', () =>
                        this.generateReportButton.click(),
                    );

                    await this.browser.allure.runStep('Дожидаемся появления кнопки загрузки отчёта', () =>
                        this.loadingButton.waitForExist(),
                    );

                    await this.browser.allure.runStep('Дожидаемся появления кнопки скачивания отчёта', () =>
                        this.downloadReportButton.waitForExist(),
                    );

                    const url = await this.downloadReportButton.getUrl();

                    await this.browser.allure.runStep('Проверяем название файла в URL', () =>
                        url.path.should.match(
                            new RegExp('stats/global/paid_opinions_report/[0-9a-z-]+/[a-zA-Z0-9-_]+.xlsx'),
                            'Название файла верное',
                        ),
                    );
                },
            }),
        },
    },
});
