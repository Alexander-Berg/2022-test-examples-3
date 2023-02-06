import {makeSuite, makeCase} from 'ginny';
import {createSurveyFormMock} from '@self/project/src/spec/hermione/helpers/metakadavr';
import {searchFeedbackFormId} from '@self/platform/configs/current/node';

const surveyState = {[searchFeedbackFormId]: createSurveyFormMock(searchFeedbackFormId)};

/**
 * Тесты на блок SearchFeedbackForm.
 * @param {PageObject.SearchFeedbackForm} SearchFeedbackForm
 */
export default makeSuite('Компонент форма качества поиска.', {
    feature: 'SearchFeedbackForm',
    environment: 'kadavr',
    story: {
        'По умолчанию': {
            'должен присутствовать': makeCase({
                id: 'marketfront-1847',
                issue: 'MARKETVERSTKA-26575',
                test() {
                    return this.searchFeedbackForm
                        .isExisting()
                        .should.eventually.to.be.equal(
                            true,
                            'Проверяем, что форма качества поиска присутствует на странице'
                        );
                },
            }),
        },

        'По нажатию на кнопку Да': {
            'должно отобразиться благодарственное сообщение': makeCase({
                id: 'marketfront-1849',
                issue: 'MARKETVERSTKA-26578',
                async test() {
                    await this.browser.setState('Forms.data.collections.forms', surveyState);

                    await this.searchFeedbackForm.clickYesButton();
                    await this.searchFeedbackForm.waitFinalStepVisible();

                    return this.searchFeedbackForm.finalStep.isVisible()
                        .should.eventually.be.equal(true, 'Благодарственное сообщение отобразилось');
                },
            }),
        },

        'По нажатию на кнопку Нет и последующем подтверждении отправки формы': {
            'должно отобразиться благодарственное сообщение': makeCase({
                id: 'marketfront-1848',
                issue: 'MARKETVERSTKA-26576',
                async test() {
                    await this.browser.setState('Forms.data.collections.forms', surveyState);

                    await this.searchFeedbackForm.clickNoButton();
                    await this.searchFeedbackForm.waitSecondStepVisible();
                    await this.searchFeedbackForm.putFeedbackComment();
                    await this.searchFeedbackForm.clickSubmitButton();
                    await this.searchFeedbackForm.waitFinalStepVisible();

                    return this.searchFeedbackForm.finalStep.isVisible()
                        .should.eventually.be.equal(true, 'Благодарственное сообщение отобразилось');
                },
            }),
        },
    },
});
