import {makeSuite, makeCase} from 'ginny';

/**
 * @param {PageObject.Form} form
 * @param {PageObject.GainedExpertise} gainedExpertise
 */

export default makeSuite('Сниппет вопроса.', {
    params: {
        expectedBadgeText: 'Текст бейджа',
    },
    story: {
        'При добавлении ответа на вопрос': {
            'появляется экран экспертизы': makeCase({
                id: 'm-touch-3314',
                async test() {
                    await this.form.setTextFieldInput('test');
                    await this.browser.yaWaitForChangeValue({
                        action: () => this.form.clickOnSendAnswerButton(),
                        valueGetter: () => this.gainedExpertise.isVisible(),
                    });

                    return this.expect(this.gainedExpertise.getBadgeText())
                        .to.be.equal(this.params.expectedBadgeText, 'Текст бейджа соответсвует ожидаемому');
                },
            }),
        },
    },
});
