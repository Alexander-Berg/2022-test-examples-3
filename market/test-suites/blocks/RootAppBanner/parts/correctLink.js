import {makeSuite, makeCase} from 'ginny';

/**
 * Тесты на блок AppPromo
 * @property {PageObject.banner} AppPromo
 */
export default makeSuite('Ссылка.', {
    story: {
        'По умолчанию.': {
            'Корректная ссылка': makeCase({
                async test() {
                    const url = await this.banner.getUrl();
                    await this.browser.allure.runStep(
                        'Проверяем, что баннер приложения содержит корректную ссылку',
                        () => this.expect(url).to.be.equal(this.params.link)
                    );
                },
            }),
        },
    },
});
