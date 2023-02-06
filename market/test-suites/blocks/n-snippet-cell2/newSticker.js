import {makeSuite, makeCase} from 'ginny';

/**
 * Тест на наличие стикера "Новинка" для продуктов isNew: true
 * @param {PageObject.SnippetCell2} snippetCell2
 */
export default makeSuite('Гридовый сниппет продукта с бейджем "Новинка".', {
    environment: 'kadavr',
    story: {
        'Бейдж "Новинка"': {
            'при наличии флага isNew': {
                'должен присутствовать': makeCase({
                    id: 'marketfront-78928',
                    async test() {
                        return this.expect(
                            await this.snippetCell2.hypeBadge.getText()
                        ).to.be.equal('НОВИНКА', 'Бейдж "Новинка" присутствует');
                    },
                }),
            },
        },
    },
});
