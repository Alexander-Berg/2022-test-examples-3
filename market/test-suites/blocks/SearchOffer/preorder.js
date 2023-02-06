import {makeCase, makeSuite} from 'ginny';

export default makeSuite('Кнопка "Предзаказ".', {
    environment: 'kadavr',
    story: {
        'Текст на кнопке "Предзаказ"': {
            'должен быть корректным': makeCase({
                async test() {
                    const text = await this.searchOffer.getCartButtonText();
                    return this.expect(text)
                        .to.equal('Предзаказ', 'Ожидаемый текст кнопки');
                },
            }),
        },
    },
});

