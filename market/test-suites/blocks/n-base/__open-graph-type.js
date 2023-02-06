import {makeSuite, makeCase} from 'ginny';

export default makeSuite('Тег meta og:type', {
    feature: 'SEO',
    story: {
        'По умолчанию': {
            'должен соответствовать ожидаемому': makeCase({
                test() {
                    const expectedOpenGraphType = this.params.expectedOpenGraphType || 'website';

                    return this.base
                        .getOpenGraphTypeContent()
                        .should.eventually.to.equal(
                            expectedOpenGraphType,
                            'Атрибут content равен переданному в параметрах'
                        );
                },
            }),
        },
    },
});
