import {makeSuite, makeCase} from 'ginny';

/**
 * Тест на работу кнопки "Предзаказ"
 */
export default makeSuite('Кнопка "Предзаказ"', {
    environment: 'kadavr',
    feature: 'Предзаказ',
    story: {
        'Текст на кнопке "Предзаказ"': {
            'должен быть корректным': makeCase({
                async test() {
                    const text = await this.cartButton.getText();
                    return this.expect(text)
                        .to.equal(this.params.expectedText, 'Ожидаемый текст кнопки');
                },
            }),
        },
        'Ссылка на кнопке "Предзаказ"': {
            'должна быть корректной': makeCase({
                async test() {
                    const link = await this.cartButton.getHref();
                    return this.expect(link)
                        .to.contain(this.params.expectedLink, 'Ожидаемая ссылка кнопки');
                },
            }),
        },
    },
});
