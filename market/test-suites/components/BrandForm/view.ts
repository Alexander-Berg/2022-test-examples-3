'use strict';

import {makeSuite, makeCase} from 'ginny';

import BrandForm from 'spec/page-objects/BrandForm';

/**
 * @param {PageObject.BrandForm} brandForm – форма редактирования бренда
 * @param {PageObject.Form} form - форма
 */
export default makeSuite('Просмотр.', {
    feature: 'Настройки',
    environment: 'kadavr',
    issue: 'VNDFRONT-3197',
    params: {
        user: 'Пользователь',
    },
    story: {
        'При наличии закрытой не позднее 2 недель назад заявки на модерацию': {
            'у полей формы отображается результат модерации': makeCase({
                issue: 'VNDFRONT-3197',
                id: 'vendor_auto-284',

                async test() {
                    await this.browser.allure.runStep(
                        'Основное решение о публикации и комментарий модератора отображаются',
                        async () => {
                            await this.brandForm.mainModerationComment
                                .vndIsExisting()
                                .should.eventually.be.equal(true, 'Основной комментарий модератора отображается');

                            await this.brandForm.mainModerationComment
                                .getText()
                                .should.eventually.be.equal(
                                    'Исправьте год основания',
                                    'Текст основного комментария корректный',
                                );

                            await this.brandForm
                                .elem(BrandForm.mainModerationStatus('success'))
                                .vndIsExisting()
                                .should.eventually.be.equal(true, 'Успешный основной статус модерации отображается');

                            await this.brandForm
                                .elem(BrandForm.mainModerationStatus('success'))
                                .getText()
                                .should.eventually.be.equal(
                                    'Публикация изменений произойдёт в течение двух рабочих дней',
                                    'Текст основного успешного статуса корректный',
                                );
                        },
                    );

                    await this.browser.allure.runStep(
                        'У поля "Страна" отображается одобрение в публикации нового значения',
                        async () => {
                            await this.brandForm
                                .getFieldModerationNewValue('country', 'success')
                                .vndIsExisting()
                                .should.eventually.be.equal(true, 'Одобрение публикации нового значения отображается');

                            await this.brandForm
                                .getFieldModerationNewValue('country', 'success')
                                .getText()
                                .should.eventually.be.equal('Соединённые Штаты Америки', 'Новое значение корректное');

                            await this.brandForm
                                .getFieldModerationComment('country', 'success')
                                .vndIsExisting()
                                .should.eventually.be.equal(
                                    true,
                                    'Комментарий к одобрению в публикации нового значения отображается',
                                );

                            await this.brandForm
                                .getFieldModerationComment('country', 'success')
                                .getText()
                                .should.eventually.be.equal(
                                    'Страна указана корректно',
                                    'Текст комментария с одобрением корректный',
                                );
                        },
                    );

                    await this.browser.allure.runStep(
                        'У поля "Год основания" отображается отказ в публикации нового значения',
                        async () => {
                            await this.brandForm
                                .getFieldModerationNewValue('foundationYear', 'denied')
                                .vndIsExisting()
                                .should.eventually.be.equal(true, 'Отказ в публикации нового значения отображается');

                            await this.brandForm
                                .getFieldModerationNewValue('foundationYear', 'denied')
                                .getText()
                                .should.eventually.be.equal('1666', 'Новое значение корректное');

                            await this.brandForm
                                .getFieldModerationComment('foundationYear', 'denied')
                                .vndIsExisting()
                                .should.eventually.be.equal(
                                    true,
                                    'Комментарий к отказу в публикации нового значения отображается',
                                );

                            await this.brandForm
                                .getFieldModerationComment('foundationYear', 'denied')
                                .getText()
                                .should.eventually.be.equal(
                                    'Год указан некорректно',
                                    'Текст комментария с отказом корректный',
                                );
                        },
                    );
                },
            }),
        },
    },
});
