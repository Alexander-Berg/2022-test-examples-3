import {makeCase, makeSuite} from 'ginny';


/*
 * Скрытие попапа настроек, если пользователь не заинтересован
 * @param {this.prompt}
 */
export default makeSuite('Отказ от попапа настроек', {
    story: {
        'Первый попап при отказе пользователя от предложения изменить настройки': {
            'должен скрываться': makeCase({
                async test() {
                    await this.prompt.waitForPromptVisible();

                    return Promise.resolve([
                        this.prompt.clickRejectButton(),
                        this.prompt.waitForPromptHidden(),
                    ]);
                },
            }),
        },
    },
});
