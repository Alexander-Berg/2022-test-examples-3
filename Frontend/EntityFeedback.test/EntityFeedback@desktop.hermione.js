'use strict';

const path = require('path');
const PO = require('./EntityFeedback.page-object');

const image = path.resolve(__dirname, 'media', 'image.png');
const video = path.resolve(__dirname, 'media', 'video.mov');
const wrongFile = path.resolve(__dirname, 'media', 'wrong.ttt');

specs({
    feature: 'Объектный ответ',
    type: 'Шторка жалоб',
}, async () => {
    describe('Внешний вид', async function() {
        beforeEach(async function() {
            await this.browser.yaOpenSerp({
                text: 'skype',
                data_filter: 'entity-search',
            }, PO.entityFooter());
            await this.browser.yaWaitForVisible(PO.entityFooter.abuseLink(), 'Не показалась ссылка Сообщить об ошибке');
            await this.browser.click(PO.entityFooter.abuseLink());
            await this.browser.yaWaitForVisible(PO.feedbackDialog(), 'Модальное окно не появилось');

            // Делаем оверлей черным, чтобы не мешало делать скриншоты, из-за текста на оверлее с прозрачностью
            await this.browser.execute(function() {
                $('.Modal-Wrapper').css('background-color', 'black');
            });
        });

        it('Закрытие модального окна по крестику', async function() {
            await this.browser.click(PO.feedbackDialog.header.close());
            await this.browser.yaWaitForHidden(PO.feedbackDialog(), 'Модальное окно должно закрыться');
        });

        it('Первая страница', async function() {
            await this.browser.assertView('plain', PO.feedbackDialog());
        });

        it('Первая страница с выбором чекбоксов', async function() {
            await this.browser.click(PO.feedbackDialog.firstCheckbox());
            await this.browser.click(PO.feedbackDialog.thirdCheckbox());

            await this.browser.assertView('plain', PO.feedbackDialog());
        });

        it('Вторая страница, без ввода email', async function() {
            await this.browser.click(PO.feedbackDialog.message());
            await this.browser.yaKeyPress('Сделайте редизайн!');
            await this.browser.click(PO.feedbackDialog.button());

            await this.browser.assertView('plain', PO.feedbackDialog());
        });

        it('Переход на предыдущий экран по стрелке "обратно"', async function() {
            await this.browser.click(PO.feedbackDialog.message());
            await this.browser.yaKeyPress('Сделайте редизайн!');
            await this.browser.click(PO.feedbackDialog.button());

            await this.browser.yaWaitForVisible(PO.feedbackDialog.header.back());
            await this.browser.click(PO.feedbackDialog.header.back());
            await this.browser.assertView('back_to_slide_1', PO.feedbackDialog());
        });

        it('Загрузка картинки', async function() {
            await this.browser.chooseFile(PO.feedbackDialog.attach.control(), image);

            await this.browser.assertView('plain', PO.feedbackDialog.wrapper());
        });

        it('Загрузка видео', async function() {
            await this.browser.chooseFile(PO.feedbackDialog.attach.control(), video);

            await this.browser.assertView('plain', PO.feedbackDialog.wrapper());
        });

        it('Загрузка картинки и видео', async function() {
            await this.browser.chooseFile(PO.feedbackDialog.attach.control(), video);
            await this.browser.chooseFile(PO.feedbackDialog.attach.control(), image);

            await this.browser.assertView('plain', PO.feedbackDialog.wrapper());
        });

        it('Загрузка неверного типа файла', async function() {
            await this.browser.chooseFile(PO.feedbackDialog.attach.control(), wrongFile);

            await this.browser.assertView('plain', PO.feedbackDialog.wrapper());
        });

        it('Вторая страница, с вводом email', async function() {
            await this.browser.click(PO.feedbackDialog.message());
            await this.browser.yaKeyPress('Сделайте редизайн!');
            await this.browser.click(PO.feedbackDialog.button());

            await this.browser.click(PO.feedbackDialog.email());
            await this.browser.yaKeyPress('hello@hello.hello');
            await this.browser.assertView('plain', PO.feedbackDialog());
        });

        it('Ввод неправильного email', async function() {
            await this.browser.click(PO.feedbackDialog.message());
            await this.browser.yaKeyPress('Сделайте редизайн!');
            await this.browser.click(PO.feedbackDialog.button());

            await this.browser.yaWaitForVisible(PO.feedbackDialog.email());
            await this.browser.click(PO.feedbackDialog.email());
            await this.browser.yaKeyPress('hello');
            await this.browser.click(PO.feedbackDialog.header());
            await this.browser.assertView('email', PO.feedbackDialog.email());
        });

        it('Третья страница', async function() {
            await this.browser.click(PO.feedbackDialog.message());
            await this.browser.yaKeyPress('Сделайте редизайн!');
            await this.browser.click(PO.feedbackDialog.button());

            await this.browser.assertView('plain', PO.feedbackDialog());
        });

        it('Четвертая страница, без ввода email', async function() {
            await this.browser.click(PO.feedbackDialog.message());
            await this.browser.yaKeyPress('Сделайте редизайн!');
            await this.browser.click(PO.feedbackDialog.button());

            await this.browser.click(PO.feedbackDialog.button());
            await this.browser.assertView('plain', PO.feedbackDialog());
        });

        it('Четвертая страница, с вводом email', async function() {
            await this.browser.click(PO.feedbackDialog.message());
            await this.browser.yaKeyPress('Сделайте редизайн!');
            await this.browser.click(PO.feedbackDialog.button());

            await this.browser.click(PO.feedbackDialog.email());
            await this.browser.yaKeyPress('hello@ya.ru');
            await this.browser.click(PO.feedbackDialog.button());
            await this.browser.moveToObject('body', 0, 0);

            await this.browser.assertView('plain', PO.feedbackDialog());
        });

        it('Дизейбл по снятию галки с политики конфиденциальности', async function() {
            await this.browser.click(PO.feedbackDialog.message());
            await this.browser.yaKeyPress('Сделайте редизайн!');
            await this.browser.click(PO.feedbackDialog.button());
            await this.browser.click(PO.feedbackDialog.firstCheckbox(), { leavePage: false });

            await this.browser.assertView('plain', PO.feedbackDialog());
        });
    });

    it('Внешний вид футера', async function() {
        await this.browser.yaOpenSerp({
            text: 'skype',
            data_filter: 'entity-search',
        }, PO.entityFooter());

        await this.browser.assertView('plain', PO.entityFooter());
    });

    it('В табе Отзывы', async function() {
        await this.browser.yaOpenSerp({
            text: 'Интерстеллар',
            intent: 'reviews',
            data_filter: 'entity-search',
        }, PO.entityFooter());

        await this.browser.yaShouldExist(PO.entityFooter());
    });

    describe('Базовые проверки', async function() {
        beforeEach(async function() {
            await this.browser.yaOpenSerp({
                text: 'skype',
                data_filter: 'entity-search',
            }, PO.entityFooter());

            await this.browser.yaWaitForVisible(PO.entityFooter.abuseLink(), 'Не показалась ссылка Сообщить об ошибке');
            await this.browser.yaCheckBaobabCounter(PO.entityFooter.abuseLink(), {
                path: '/$page/$parallel/$result/FeedbackFooter/abuse',
            });

            await this.browser.yaWaitForVisible(PO.feedbackDialog(), 'Шторка не появилась');
            await this.browser.click(PO.feedbackDialog.message());

            await this.browser.yaKeyPress('Сделайте редизайн!');
            await this.browser.yaCheckBaobabCounter(PO.feedbackDialog.button(), {
                path: '/$page/$parallel/$result/FeedbackFooter/feedback-submit',
            });
        });

        it('Без ввода email', async function() {
            await this.browser.yaCheckBaobabCounter(PO.feedbackDialog.button(), {
                path: '/$page/$parallel/$result/FeedbackFooter/email-submit',
            });

            await this.browser.yaCheckBaobabCounter(PO.feedbackDialog.button(), {
                path: '/$page/$parallel/$result/FeedbackFooter/feedback-sended',
            });
        });

        it('С вводом email', async function() {
            await this.browser.click(PO.feedbackDialog.email());
            await this.browser.yaKeyPress('hello@ya.ru');

            await this.browser.yaCheckBaobabCounter(PO.feedbackDialog.button(), {
                path: '/$page/$parallel/$result/FeedbackFooter/email-submit',
            });

            await this.browser.yaCheckBaobabCounter(PO.feedbackDialog.lastButton(), {
                path: '/$page/$parallel/$result/FeedbackFooter/feedback-sended',
            });
        });

        it('Изменение email', async function() {
            await this.browser.click(PO.feedbackDialog.email());
            await this.browser.yaKeyPress('hello@ya.ru');

            await this.browser.yaCheckBaobabCounter(PO.feedbackDialog.button(), {
                path: '/$page/$parallel/$result/FeedbackFooter/email-submit',
            });

            await this.browser.yaCheckBaobabCounter(PO.feedbackDialog.button(), {
                path: '/$page/$parallel/$result/FeedbackFooter/change-email',
            });
        });

        it('Ссылка и счетчик на политику конфиденциальности', async function() {
            await this.browser.yaCheckLink2({
                selector: PO.feedbackDialog.link(),
                url: {
                    href: {
                        url: 'https://yandex.ru/legal/confidential/',
                        ignore: ['protocol'],
                        target: '_blank',
                        queryValidator: query => query,
                    },
                },
                baobab: {
                    path: '/$page/$parallel/$result/FeedbackFooter/confidential',
                },
                message: 'Сломана ссылка на политику конфиденциальности',
            });
        });
    });

    describe('Доступность', async function() {
        beforeEach(async function() {
            await this.browser.yaOpenSerp({
                text: 'skype',
                data_filter: 'entity-search',
            }, PO.entityFooter());

            await this.browser.yaWaitForVisible(PO.entityFooter.abuseLink(), 'Не показалась ссылка Сообщить об ошибке');
            await this.browser.click(PO.entityFooter.abuseLink());
            await this.browser.yaWaitForVisible(PO.feedbackDialog(), 'Шторка не появилась');
        });

        // Если фокус будет за пределами окошка, то пользователю будет трудно сориентироваться
        it('При открытии жалобщика фокус стоит на элементе внутри окна', async function() {
            const closeButtonHasFocus = await this.browser.hasFocus(PO.feedbackDialog.header.close());
            assert.isTrue(closeButtonHasFocus, 'Отсутствует фокус на крестике');
        });

        it('При вводе почты фокус стоит на элементе внутри окна', async function() {
            await this.browser.click(PO.feedbackDialog.message());
            await this.browser.yaKeyPress('Сделайте редизайн!');
            await this.browser.click(PO.feedbackDialog.lastButton());

            const buttonHasFocus = await this.browser.hasFocus(PO.feedbackDialog.lastButton());
            assert.isTrue(buttonHasFocus, 'Отсутствует фокус на кнопке');
        });

        it('На последнем экране фокус стоит на элементе внутри окна', async function() {
            await this.browser.click(PO.feedbackDialog.message());
            await this.browser.yaKeyPress('Сделайте редизайн!');
            await this.browser.click(PO.feedbackDialog.lastButton());
            await this.browser.click(PO.feedbackDialog.email());
            await this.browser.yaKeyPress('hello@ya.ru');
            await this.browser.click(PO.feedbackDialog.lastButton());

            const buttonHasFocus = await this.browser.hasFocus(PO.feedbackDialog.lastButton());
            assert.isTrue(buttonHasFocus, 'Отсутствует фокус на кнопке');
        });

        it('Диалоговое окно связано с заголовком', async function() {
            const ariaLabelledBy = await this.browser.getAttribute(PO.modal.content(), 'aria-labelledby');
            const titleId = await this.browser.getAttribute(PO.feedbackDialog.header.title(), 'id');

            assert.strictEqual(titleId, ariaLabelledBy, 'Жалобщик должен быть связан с его заголовком');
        });
    });
});
