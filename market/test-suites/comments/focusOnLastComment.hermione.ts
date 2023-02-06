import 'hermione';

import {login} from '../../helpers';
import Comment from '../../page-objects/comment';

/**
 * План теста:
 * 1. Перейти на страницу существующего обращения
 * 2. Дождаться загрузки блока с комментариями
 * 3. Проверить, что последний комментарий из всего блока виден на странице
 */

/** ссылка на любое обращение */
const PAGE_URL = '/entity/ticket@94234723';

describe('ocrm-493: Обращение открывается с фокусом на последнем сообщении', () => {
    beforeEach(function() {
        return login(PAGE_URL, this);
    });

    it('проверяет, что последний комментарий виден на странице', async function() {
        const lastComment = new Comment(this.browser, 'body', '[data-ow-test-comment]:last-of-type');

        await lastComment.isDisplayed('Последний комментарий отсутствует на экране');
    });
});
