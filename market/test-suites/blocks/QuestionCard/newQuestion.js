import {makeCase, makeSuite} from 'ginny';

/**
 * @param {PageObject.ProductQuestionPage | PageObject.CategoryQuestionPage} questionPage
 */
export default makeSuite('Блок снипета вопроса, когда вопрос только что создан', {
    story: {
        'по умолчанию': {
            'зелеблок отображается': makeCase({
                id: 'm-touch-2244',
                issue: 'MOBMARKET-9106',
                feature: 'Структура страницы',
                async test() {
                    const isGreenBlockVisible = await this.questionPage.isInlineNotificationVisible();
                    await this.expect(isGreenBlockVisible).to.be.equal(true, 'Зелеблок отображается');
                },
            }),
        },
    },
});
