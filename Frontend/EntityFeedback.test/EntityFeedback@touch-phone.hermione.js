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
    describe('Внешний вид', function() {
        beforeEach(async function() {
            await this.browser.yaOpenSerp({
                text: 'skype',
                data_filter: 'entity-search',
            }, PO.entityFooter());
            await this.browser.yaWaitForVisible(PO.entityFooter.abuseLink(), 'Не показалась ссылка Сообщить об ошибке');
            await this.browser.click(PO.entityFooter.abuseLink());
            await this.browser.yaWaitForVisible(PO.feedbackDialog(), 'Шторка не появилась');
        });

        hermione.also.in('iphone-dark');
        it('Тайтл с разделителем', async function() {
            await this.browser.execute(function() {
                $('.Drawer-Overlay').css('background-color', 'black');
            });
            await this.browser.yaAssertViewExtended('plain', PO.feedbackDialog.header(), { verticalOffset: 5 });
        });

        hermione.only.in('iphone', 'Загрузка файлов стабильно работает только на iphone');
        it('Загрузка картинки', async function() {
            await this.browser.chooseFile(PO.feedbackDialog.attach.control(), image);

            await this.browser.assertView('plain', PO.feedbackDialog.wrapper());
        });

        hermione.only.in('iphone', 'Загрузка файлов стабильно работает только на iphone');
        it('Загрузка видео', async function() {
            await this.browser.chooseFile(PO.feedbackDialog.attach.control(), video);

            await this.browser.assertView('plain', PO.feedbackDialog.wrapper());
        });

        hermione.only.in('iphone', 'Загрузка файлов стабильно работает только на iphone');
        it('Загрузка картинки и видео', async function() {
            await this.browser.chooseFile(PO.feedbackDialog.attach.control(), video);
            await this.browser.chooseFile(PO.feedbackDialog.attach.control(), image);

            await this.browser.assertView('plain', PO.feedbackDialog.wrapper());
        });

        hermione.only.in('iphone', 'Загрузка файлов стабильно работает только на iphone');
        it('Загрузка неверного типа файла', async function() {
            await this.browser.chooseFile(PO.feedbackDialog.attach.control(), wrongFile);

            await this.browser.assertView('plain', PO.feedbackDialog.wrapper());
        });

        it('Первая страница с выбором чекбоксов', async function() {
            await this.browser.click(PO.feedbackDialog.firstCheckbox());
            await this.browser.click(PO.feedbackDialog.thirdCheckbox());

            await this.browser.assertView('plain', PO.feedbackDialog.checkboxList());
        });

        it('Первая страница с выбором чекбокса "Другое"', async function() {
            await this.browser.click(PO.feedbackDialog.fifthCheckbox());

            await this.browser.assertView('plain', PO.feedbackDialog.message());
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
            await this.browser.assertView('back_to_slide_1', PO.feedbackDialog.header());
        });

        it('Дизейбл по снятию галки с политики конфиденциальности', async function() {
            await this.browser.click(PO.feedbackDialog.message());
            await this.browser.yaKeyPress('Сделайте редизайн!');
            await this.browser.click(PO.feedbackDialog.button());
            await this.browser.execute(element => {
                $(element).removeAttr('href');
            }, PO.feedbackDialog.link());
            await this.browser.click(PO.feedbackDialog.firstCheckbox());

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

    describe('Базовые проверки', async function() {
        beforeEach(async function() {
            await this.browser.yaOpenSerp({
                text: 'skype',
                data_filter: 'entity-search',
            }, PO.entityFooter());

            await this.browser.yaWaitForVisible(PO.entityFooter.abuseLink(), 'Не показалась ссылка Сообщить об ошибке');
            await this.browser.yaCheckBaobabCounter(PO.entityFooter.abuseLink(), {
                path: '/$page/$main/$result/FeedbackFooter/abuse',
            });

            await this.browser.yaWaitForVisible(PO.feedbackDialog(), 'Шторка не появилась');
            await this.browser.click(PO.feedbackDialog.message());

            await this.browser.yaKeyPress('Сделайте редизайн!');
            await this.browser.yaCheckBaobabCounter(PO.feedbackDialog.button(), {
                path: '/$page/$main/$result/FeedbackFooter/feedback-submit',
            });
        });

        it('Без ввода email', async function() {
            await this.browser.yaCheckBaobabCounter(PO.feedbackDialog.button(), {
                path: '/$page/$main/$result/FeedbackFooter/email-submit',
            });

            await this.browser.yaCheckBaobabCounter(PO.feedbackDialog.button(), {
                path: '/$page/$main/$result/FeedbackFooter/feedback-sended',
            });
        });

        it('С вводом email', async function() {
            await this.browser.click(PO.feedbackDialog.email());
            await this.browser.yaKeyPress('hello@ya.ru');

            await this.browser.yaCheckBaobabCounter(PO.feedbackDialog.button(), {
                path: '/$page/$main/$result/FeedbackFooter/email-submit',
            });

            await this.browser.yaCheckBaobabCounter(PO.feedbackDialog.lastButton(), {
                path: '/$page/$main/$result/FeedbackFooter/feedback-sended',
            });
        });

        it('Изменение email', async function() {
            await this.browser.click(PO.feedbackDialog.email());
            await this.browser.yaKeyPress('hello@ya.ru');

            await this.browser.yaCheckBaobabCounter(PO.feedbackDialog.button(), {
                path: '/$page/$main/$result/FeedbackFooter/email-submit',
            });

            await this.browser.yaCheckBaobabCounter(PO.feedbackDialog.button(), {
                path: '/$page/$main/$result/FeedbackFooter/change-email',
            });
        });
    });
});
