import 'hermione';

import {expect} from 'chai';

import {createOutgoingTicket} from '../../helpers';
import Comment from '../../page-objects/comment';

const TEXT = 'comment';

describe('ocrm-951: Введённый текст сообщения сохраняется при уходе на другую страницу системы', () => {
    it('При уходе со страницы обращения и возвращении на неё, текст введенного комментария сохраняется', async function() {
        await createOutgoingTicket(this);

        const comment = new Comment(this.browser, 'body', '[data-ow-test-content="comments"] [role="textbox"]');

        await comment.isDisplayed();

        await comment.addComment(TEXT);

        const rootPageLink = await this.browser.$('aside li[title="Письменная коммуникация"] a');

        await rootPageLink.isDisplayed();

        await rootPageLink.click();

        await comment.isNotDisplayed();

        await this.browser.back();

        await comment.isDisplayed();

        const textFieldValue = await comment.getCommentText();

        expect(textFieldValue).to.equal(
            TEXT,
            'Текст сообщения либо не сохранился в поле ввода, либо отличается от ожидаемого.'
        );
    });
});
