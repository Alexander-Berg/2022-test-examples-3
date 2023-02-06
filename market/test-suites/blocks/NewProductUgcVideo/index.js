import {makeSuite, mergeSuites, makeCase} from 'ginny';

/**
 * Тест блока добавления UGC видео
 * @param {PageObject.NewProductUgcVideoContent} content
 * @param {PageObject.UgcVideoInput} videoInput
 */
export default makeSuite('Блок добавления UGC видео.', {
    story: {
        'Сниппет товара': {
            'при клике': {
                'перенаправляет на страницу товара': makeCase({
                    id: 'm-touch-3405',
                    issue: 'MARKETFRONT-9167',
                    async test() {
                        return this.browser.yaWaitForPageReloadedExtended(
                            () => this.content.clickProductTitle()
                        ).then(() => Promise.all([
                            this.browser.getUrl(),
                            this.browser.yaBuildURL('touch:product', {
                                productId: this.params.productId,
                                slug: this.params.slug,
                            }),
                        ])
                        ).then(([openedUrl, expectedPath]) => this
                            .expect(openedUrl, 'Проверяем что произошел переход на правильную страницу')
                            .to.be.link(expectedPath, {
                                skipProtocol: true,
                                skipHostname: true,
                            })
                        );
                    },
                }),
            },
        },
        'Видео загружено.': mergeSuites(
            {
                async beforeEach() {
                    await this.browser.yaWaitForChangeValue({
                        action: () => this.videoInput.chooseVideo(),
                        valueGetter: () => this.videoInput.isVideoLoaded(),
                    });
                },
            },
            {
                'Кнопка "Опубликовать"': {
                    'при пустом описании': {
                        'недоступна': makeCase({
                            id: 'm-touch-3406',
                            issue: 'MARKETFRONT-9167',
                            async test() {
                                return this.content.publishButton.isEnabled()
                                    .should.eventually.to.be.equal(
                                        false,
                                        'Кнопка "Опубликовать" должна быть недоступна'
                                    );
                            },
                        }),
                    },
                    'если в описании один символ': {
                        'доступна': makeCase({
                            id: 'm-touch-3407',
                            issue: 'MARKETFRONT-9167',
                            async test() {
                                await this.browser.yaWaitForChangeValue({
                                    action: () => this.content.setReviewText('a'),
                                    valueGetter: () => this.content.publishButton.isEnabled(),
                                });
                                return this.content.publishButton.isEnabled()
                                    .should.eventually.to.be.equal(
                                        true,
                                        'Кнопка "Опубликовать" должна быть доступна'
                                    );
                            },
                        }),
                    },
                    'если в описании 101 символ': {
                        'недоступна': makeCase({
                            id: 'm-touch-3408',
                            issue: 'MARKETFRONT-9167',
                            async test() {
                                await this.browser.yaWaitForChangeValue({
                                    action: () => this.content.setReviewText('a'),
                                    valueGetter: () => this.content.publishButton.isEnabled(),
                                });
                                await this.browser.yaWaitForChangeValue({
                                    action: () => this.content.setReviewText('a'.repeat(101)),
                                    valueGetter: () => this.content.publishButton.isEnabled(),
                                });
                                await this.content.publishButton.isEnabled()
                                    .should.eventually.to.be.equal(
                                        false,
                                        'Кнопка "Опубликовать" должна быть недоступна'
                                    );
                            },
                        }),
                    },
                },
            },
            {
                'Поле "Описание"': {
                    'при нажатии на крестик': {
                        'очищается': makeCase({
                            id: 'm-touch-3409',
                            issue: 'MARKETFRONT-9167',
                            async test() {
                                await this.browser.yaWaitForChangeValue({
                                    action: () => this.content.setReviewText('a'),
                                    valueGetter: () => this.content.publishButton.isEnabled(),
                                });
                                await this.browser.yaWaitForChangeValue({
                                    action: () => this.content.setReviewText('a'.repeat(101)),
                                    valueGetter: () => this.content.publishButton.isEnabled(),
                                });
                                await this.content.publishButton.isEnabled()
                                    .should.eventually.to.be.equal(
                                        false,
                                        'Кнопка "Опубликовать" должна быть недоступна'
                                    );
                                await this.browser.yaWaitForChangeValue({
                                    action: () => this.content.clickResetButton(),
                                    valueGetter: () => this.content.getReviewText(),
                                });
                                await this.content.getReviewText()
                                    .should.eventually.to.be.equal(
                                        '',
                                        'Поле ввода очищается'
                                    );
                            },
                        }),
                    },
                },
            }
        ),
    },
});
