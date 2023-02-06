import {makeSuite, makeCase} from 'ginny';

/**
 * @param {PageObject.Form} form
 * @param {PageObject.GainedExpertise} gainedExpertise
 */

export default makeSuite('Сниппет вопроса.', {
    params: {
        expectedBadgeText: 'Ожидаемый текст бейджа',
    },
    story: {
        'При добавлении ответа на вопрос': {
            'появляется экран экспертизы с бейджом с корректным текстом': makeCase({
                id: 'marketfront-4060',
                async test() {
                    await this.form.setText('a');
                    await this.browser.yaWaitForChangeValue({
                        action: () => this.form.clickSubmitButton(),
                        valueGetter: () => this.gainedExpertise.isVisible(),
                    });

                    return this.expect(this.gainedExpertise.getBadgeText())
                        .to.be.equal(this.params.expectedBadgeText, 'Текст бейджа правильный');
                },
            }),
        },
    },
});
