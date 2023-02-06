'use strict';

import {makeSuite, makeCase} from 'ginny';

// eslint-disable-next-line  max-len
const RUS_LETTER_LINK =
    'https://vendors-public.s3.yandex.net/offer-documents/2d5208a5-eb96-4e15-8ffd-bda01ba8f96b/letter_rus.docx';
// eslint-disable-next-line  max-len
const ENG_LETTER_LINK =
    'https://vendors-public.s3.yandex.net/offer-documents/a3782985-a2d2-4bff-9f39-32c311f44a78/letter_eng.doc';

/**
 * Тест ссылок на скачивание шаблонов гарантийного письма
 *
 * @param {PageObject.Form} form - форма
 * @param {PageObject.DocumentUpload} guaranteeLetter - блок с загрузкой гарантийного письма
 *
 */
export default makeSuite('Шаблоны гарантийного письма.', {
    environment: 'testing',
    params: {
        user: 'Пользователь',
    },
    story: {
        'В инструкции по добавлению гарантийного письма': {
            'содержатся корректные ссылки на скачивание шаблонов': makeCase({
                async test() {
                    await this.form
                        .getDocumentUploadFieldByName('guaranteeLetters')
                        .vndIsExisting()
                        .should.eventually.be.equal(true, 'Поле "Гарантийные письма" отображается');

                    await this.form
                        .link('Добавить гарантийное письмо')
                        .vndIsExisting()
                        .should.eventually.be.equal(true, 'Кнопка "Добавить гарантийное письмо" отображается');

                    await this.form.clickAddNewDocument('Добавить гарантийное письмо');

                    await this.browser.allure.runStep('Дожидаемся появления блока загрузки гарантийного письма', () =>
                        this.guaranteeLetter.waitForExist(),
                    );

                    await this.browser.allure.runStep(
                        'Проверяем корректность отображения блока загрузки гарантийного письма',
                        async () => {
                            await this.guaranteeLetter.uploadButton
                                .vndIsExisting()
                                .should.eventually.be.equal(true, 'Кнопка загрузки документа отображается');

                            await this.guaranteeLetter.instructions
                                .vndIsExisting()
                                .should.eventually.be.equal(true, 'Инструкция по загрузке файлов отображается');

                            await this.guaranteeLetter.instructions
                                .getText()
                                .should.eventually.includes(
                                    'Скачайте шаблон гарантийного письма',
                                    'Текст инструкции по загрузке файлов корректный',
                                );

                            await this.guaranteeLetter.resetButton
                                .vndIsExisting()
                                .should.eventually.be.equal(true, 'Кнопка удаления файла отображается');
                        },
                    );

                    await this.guaranteeLetter
                        .getTemplateLinkHref('rus')
                        .should.eventually.be.equal(RUS_LETTER_LINK, 'Ссылка на шаблон письма на русском корректная');

                    await this.guaranteeLetter
                        .getTemplateLinkHref('eng')
                        .should.eventually.be.equal(
                            ENG_LETTER_LINK,
                            'Ссылка на шаблон письма на английском корректная',
                        );
                },
            }),
        },
    },
});
