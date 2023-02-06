import {makeCase, makeSuite, mergeSuites, prepareSuite} from 'ginny';
import AgitationCardMenu from '../AgitationCardMenu/AgitationCardMenu';

/**
 * @param {PageObject.components.QuestionAgitationCard} questionAgitationCard
 * @param {PageObject.components.AgitationCard} agitationCard
 * @param {PageObject.components.AgitationCardMenu} agitationCardMenu
 */
export default makeSuite('Сниппет вопроса.', {
    params: {
        questionUrl: 'URL страницы вопроса на которую ведет агитация',
        question: 'Текст вопроса',
        productUrl: 'URL КО продукта',
        productName: 'Название продукта',
        questionAuthor: 'Автор вопроса',
    },
    story: mergeSuites({
        'По умолчанию': {
            'отображается': makeCase({
                id: 'marketfront-3964',
                test() {
                    return this.questionAgitationCard.isVisible()
                        .should.eventually.be.equal(true, 'Сниппет вопроса отображается');
                },
            }),
            'содержит корректный текст вопроса': makeCase({
                id: 'marketfront-3965',
                test() {
                    return this.questionAgitationCard.questionText.getText()
                        .should.eventually.be.equal(this.params.question, 'Сниппет вопроса содержит корректный текст');
                },
            }),
            'содержит корректное название товара': makeCase({
                id: 'marketfront-3966',
                test() {
                    return this.questionAgitationCard.productTitle.getText()
                        .should.eventually.be.equal(this.params.productName,
                            'Сниппет вопроса содержит корректное название товара');
                },
            }),
            'содержит корректного автора': makeCase({
                id: 'marketfront-3967',
                test() {
                    return this.questionAgitationCard.author.getText()
                        .should.eventually.be.equal(this.params.questionAuthor,
                            'Сниппет вопроса отображает правильного автора вопроса');
                },
            }),
        },
        'Клик по кнопке ответить': {
            'ведет на правильную страницу вопроса': makeCase({
                id: 'marketfront-3968',
                async test() {
                    await this.questionAgitationCard.answerButton.click();

                    return this.browser.getUrl()
                        .should.eventually.be.link({
                            pathname: this.params.questionUrl,
                        }, {
                            skipProtocol: true,
                            skipHostname: true,
                        });
                },
            }),
        },
        'Клик по названию товар': {
            'ведет на карточку модели продукта': makeCase({
                id: 'marketfront-3969',
                async test() {
                    await this.questionAgitationCard.productTitle.click();

                    return this.browser.getUrl()
                        .should.eventually.be.link({
                            pathname: this.params.productUrl,
                        }, {
                            skipProtocol: true,
                            skipHostname: true,
                        });
                },
            }),
        },
        'Клик по изображению продукта': {
            'ведет на карточку модели продукта': makeCase({
                id: 'marketfront-3970',
                async test() {
                    await this.questionAgitationCard.productImage.click();

                    return this.browser.getUrl()
                        .should.eventually.be.link({
                            pathname: this.params.productUrl,
                        }, {
                            skipProtocol: true,
                            skipHostname: true,
                        });
                },
            }),
        },
    },
    prepareSuite(AgitationCardMenu)),
});
