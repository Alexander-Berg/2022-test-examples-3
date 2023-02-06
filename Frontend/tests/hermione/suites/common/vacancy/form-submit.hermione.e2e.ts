describe('Страница вакансии', function() {
    it('Сабмит формы', async function() {
        // @ts-ignore
        const { browser, PO } = this;
        // Здесь при первом переходе почему-то открывается по протоколу http,
        // несмотря на то, что явно указано иное. С другими страницами (не-jobs) этого не происходит,
        // может быть связано с настройками балансера. Однако переключение домена для hamster
        // на конфигурацию с принудительным http -> https в результате приводит к циклическим редиректам,
        // поэтому...
        await browser.url('/jobs/');
        await browser.url('/jobs/vacancies/34');

        await browser.scroll('body', 0, 9999);
        await browser.yaWaitForVisible(PO.lcYandexForm(), 5000, 'На странице вакансии отсутствует форма');

        await browser.switchToFrame(await browser.findElement('xpath', '//iframe[@class="lc-iframe__iframe"]'));
        // Не разрешает брать у элемента св-во ELEMENT. А yaClickAtTheMiddle, увы, тут не помог.
        // @ts-ignore
        await browser.elementClick((await browser.findElement('xpath', '//button[contains(@class, "button_role_submit")]')).ELEMENT);

        await browser.switchToParentFrame();

        await browser.yaWaitForVisible(`#gruppa-uspeha ${PO.lcJobsInfoblock()}`, 'После сабмита формы не появилось сообщение об успехе');
    });
});
