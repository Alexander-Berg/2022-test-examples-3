'use strict';

const PO = require('./EntityFeedback.page-object');
const mockOptions = {
    recordData: ['/drawer-support/tickets/new'],
    urlDataMap: {
        ['/drawer-support/tickets/new']: {},
    },
};

specs({
    feature: 'Объектный ответ',
    type: 'Шторка жалоб',
}, async () => {
    it('Проверка отправленных данных', async function() {
        await this.browser.yaOpenSerp({
            text: 'skype',
            data_filter: 'entity-search',
        }, PO.entityFooter());

        await this.browser.yaWaitForVisible(PO.entityFooter.abuseLink(), 'Не показалась ссылка Сообщить об ошибке');
        await this.browser.click(PO.entityFooter.abuseLink());
        await this.browser.yaWaitForVisible(PO.feedbackDialog(), 'Шторка не открылась');

        await this.browser.click(PO.feedbackDialog.firstCheckbox());
        await this.browser.click(PO.feedbackDialog.button());

        await this.browser.click(PO.feedbackDialog.email());
        await this.browser.yaKeyPress('hello@ya.ru');
        await this.browser.click(PO.feedbackDialog.button());

        await this.browser.yaMockXHR(mockOptions);
        await this.browser.click(PO.feedbackDialog.lastButton());

        const ajaxRecords = await this.browser.yaGetXHRRecords('/drawer-support/tickets/new');
        const ajaxBody = ajaxRecords[0].body;

        assert.equal(ajaxRecords[0].method, 'POST', 'Запрос должен отправляться методом POST');

        assert.equal(ajaxBody.message, '', 'Тело сообщения не пустое');
        assert.equal(ajaxBody.metaFields.feature, 'Объектный ответ', 'Ошибка в параметре feature');
        assert.equal(ajaxBody.metaFields.email, 'hello@ya.ru', 'Ошибка в параметре email');
        assert.exists(ajaxBody.metaFields.docId, 'Отсутствует номер документа на выдаче');

        assert.exists(ajaxBody.metaFields.uid, 'Отсутствует uid');
        assert.exists(ajaxBody.metaFields.yandexuid, 'Отсутствует yandexuid');

        assert.equal(ajaxBody.metaFields.fields[0].name, 'ento', 'Ожидается энтушка в кастомных полях');
        assert.exists(ajaxBody.metaFields.fields[0].value, 'Ошибка в кастомной поле ento');

        assert.equal(ajaxBody.fields[0].name, 'Выбранные категории', 'Отправленное не верное название категорий');
        assert.equal(ajaxBody.fields[0].value, 'Картинка не подходит', 'Отправлено не верное значение чекбокса');

        assert.deepEqual(ajaxBody.metaFields.categories, ['Картинка не подходит'], 'Не заполнены категории жалобы');
    });
});
