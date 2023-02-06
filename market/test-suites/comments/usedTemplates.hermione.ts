import 'hermione';
import {expect} from 'chai';

import Button from '../../page-objects/button';
import PopperNew from '../../page-objects/popperNew';
import {checkAllUsedTemplates} from './helpers/checkAllUsedTemplates';
import {createOutgoingTicket, login, turnOffExperiment} from '../../helpers';

/**
 * План теста:
 * 1. Перейти на страницу создания обращения
 * 2. Заполнить все обязательные поля
 * 3. Сохранить обращение и проверить что мы перешли на страницу просмотра
 * 4. Нажать на кнопку «Выбрать ответ из шаблона"
 * 5. Выбрать шаблон
 * 6. Использовать этот шаблон
 * 7. Отправить комментарий с шаблоном
 * 8. Перейти на страницу очереди «Покупки > Общие вопросы"
 * 9. Найти обращение из п.1
 * 10. Настроить таблицу, чтобы в ней отображался аттрибут allUsedTemplates
 * 11. Проверить, что в обращении есть использованный шаблон
 */

describe('ocrm-1508: Информация о примененных шаблонах сохраняется в обращении', () => {
    beforeEach(function() {
        return login('', this);
    });

    it('при добавлении шаблонов в обращении, они отображаются в колонке: Все примененные шаблоны', async function() {
        await turnOffExperiment(this.browser);
        const createdTicketTitle = await createOutgoingTicket(this);

        const addTemplateButton = new Button(this.browser, 'body', '[data-ow-test-template-select]');
        const template = new Button(this.browser, 'body', '[data-ow-test-template="Шаблон для автотестов"]');
        const useTemplateButton = new Button(this.browser, 'body', '[data-ow-test-use-template]');
        const addCommentButton = new Button(this.browser, 'body', '[data-ow-test-add-comment]');
        const commentActions = new PopperNew(this.browser);

        await addTemplateButton.clickButton();

        await template.clickButton();
        await useTemplateButton.clickButton();

        const modalClosed = await useTemplateButton.waitForInvisible();

        expect(modalClosed).to.equal(true, 'При добавлении шаблона произошла ошибка');

        await addCommentButton.clickButton();
        await commentActions.isDisplayed();
        await commentActions.clickFirstElement();

        const commentSent = await addCommentButton.waitForEnable();

        expect(commentSent).to.equal(true, 'При отправке комментария произошла ошибка');

        await checkAllUsedTemplates(this, createdTicketTitle, 'Шаблон для автотестов');
    });
});
