import {makeCase, makeSuite} from 'ginny';

/**
 * Тесты на блок CameraInput
 * @property {PageObject.CameraInput} cameraInput
 */
export default makeSuite('Кнопка загрузки фотографии.', {
    story: {
        'При загрузке': {
            'должна редиректить на страницу поиска по картинке': makeCase({
                id: 'm-touch-2766',
                issue: 'MOBMARKET-12109',
                async test() {
                    await this.cameraInput.click();


                    const changedUrl = await this.browser
                        .yaWaitForChangeUrl(() => this.cameraInput.chooseFile(`${__dirname}/iphone.webp`), 10000);

                    const expectedUrl = await this.browser
                        .yaBuildURL('touch:picsearch');

                    return this.browser.allure.runStep(
                        'Проверяем, что перешли на страницу поиска по картинке',
                        () => this.expect(changedUrl, 'Перешли на страницу поиска по картинке')
                            .to.be.link(expectedUrl, {
                                skipProtocol: true,
                                skipHostname: true,
                            })
                    );
                },
            }),
        },
    },
});
