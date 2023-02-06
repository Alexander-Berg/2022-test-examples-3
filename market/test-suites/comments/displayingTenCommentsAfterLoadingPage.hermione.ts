import 'hermione';
import {expect} from 'chai';

import {login} from '../../helpers';
import Comment from '../../page-objects/comment';

/**
 * План теста:
 * 1. Перейти на страницу существующего обращения
 * 2. Дождаться загрузки блока с комментариями
 * 3. Проверить, что загрузилось 10 комментариев
 */

/** ссылка на любое обращение */
const PAGE_URL = '/entity/ticket@94234723';

/** селектор комментария */
const COMMENT_SELECTOR = '[data-ow-test-comment]';

describe('ocrm-1328: Отображение 10 комментариев при загрузке страницы обращения', () => {
    beforeEach(function() {
        return login(PAGE_URL, this);
    });

    it('проверяет, что количество комментариев на странице соответствует ожидаемому', async function() {
        const commentsList = new Comment(this.browser, 'body', COMMENT_SELECTOR);

        await commentsList.isDisplayed('Комментарии не прогрузились');

        const commentCount = (await this.browser.$$(COMMENT_SELECTOR)).length;

        expect(commentCount).to.be.equal(10, 'Количество комментариев отличается от ожидаемого');
    });
});
