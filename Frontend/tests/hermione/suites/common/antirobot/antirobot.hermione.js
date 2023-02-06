describe('Антиробот', () => {
    describe('Страница задачи', function() {
        const requestDuration = 3000;

        beforeEach(function() {
            return this.browser.yaOpenPage('/subject/problem/?problem_id=T979&show-captcha=1')
                .click(PO.TaskControlLine.button())
                .yaWaitForVisible(PO.CaptchaModal());
        });

        it('Внешний вид', function() {
            return this.browser
                .assertView('captcha', PO.CaptchaModal.Form());
        });

        it('Удачный ввод капчи', function() {
            return this.browser
                .setValue(PO.CaptchaModal.input(), 'success')
                .click(PO.CaptchaModal.Button())
                .waitForExist(
                    PO.TextInputWrong(),
                    requestDuration,
                );
        });
        it('Неудачный ввод капчи', function() {
            return this.browser
                .setValue(PO.CaptchaModal.input(), 'failed')
                .click(PO.CaptchaModal.Button())
                .yaWaitForVisible(PO.CaptchaModal());
        });
    });
});
