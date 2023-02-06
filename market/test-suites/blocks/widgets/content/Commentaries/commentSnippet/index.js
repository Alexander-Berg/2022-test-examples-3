import {makeSuite, makeCase} from 'ginny';

/**
 * @param {PageObject.CommentSnippet} commentSnippet
 */
export default makeSuite('Сниппет комментария.', {
    feature: 'Комментарии к статьям',
    story: {
        'Нажатие на "читать дальше"': {
            'раскрывает весь комментарий': makeCase({
                id: 'm-touch-2511',
                issue: 'MOBMARKET-10729',
                async test() {
                    await this.commentSnippet.isSnippetPreview().should.eventually.to.be.equal(true, 'текст cвёрнут');
                    await this.commentSnippet.expandText();
                    const fullText = await this.commentSnippet.getText();
                    await this.expect(fullText).to.equal('a'.repeat(5000), 'полный текст комментария корректный');
                },
            }),
        },
    },
});
