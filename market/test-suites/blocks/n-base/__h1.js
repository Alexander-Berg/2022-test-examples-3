import {makeSuite, makeCase} from 'ginny';

export default makeSuite('Заголовок первого уровня', {
    feature: 'SEO',
    story: {
        'По умолчанию': {
            'должен соответствовать ожидаемому.': makeCase({
                params: {
                    expectedH1Title: 'Ожидаемый h1 заголовок страницы',
                },
                test() {
                    return this.base
                        .h1.getText()
                        .should.eventually.to.equal(
                            this.params.expectedH1Title,
                            `заголовок h1 должен быть равен - ${this.params.expectedH1Title}`
                        );
                },
            }),
        },
    },
});
