const url = '/subject/problem/?problem_id=T1507&print=1&show-captcha=1&show_content=1';
const pdfUrl = 'https://yandex.ru';
const ANIMATION_DELAY = 1000;

hermione.only.notIn('linux-chrome-ipad', 'нет такой функциональности');
describe('Антиробот', () => {
    describe('Страница печати', function() {
        beforeEach(function() {
            return this.browser
                .yaOpenPage(url)
                .yaWaitForVisible(PO.PrintPdfToolbar.OrientationSelect())
                .click(PO.PrintPdfToolbar.OrientationSelect())
                .pause(ANIMATION_DELAY)
                .click(PO.PrintPdfToolbarControlMenu.Portrait())
                .click(PO.PrintPdfToolbar.Download())
                .yaWaitForVisible(PO.CaptchaModal());
        });

        it('Внешний вид', function() {
            return this.browser
                .assertView('captcha', PO.CaptchaModal.Form());
        });

        it('Удачный ввод капчи', function() {
            let currentUrl;

            return this.browser
                .getUrl().then(url => currentUrl = url)
                .setValue(PO.CaptchaModal.input(), 'success')
                .click(PO.CaptchaModal.Button())
                .waitUntil(() => {
                    return this.browser.getUrl().then(url => {
                        return url !== currentUrl;
                    });
                }, 10000, 'Не произошел редирект на страницу с pdf')
                .yaCheckPageUrl(
                    pdfUrl,
                    { skipQuery: true },
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
