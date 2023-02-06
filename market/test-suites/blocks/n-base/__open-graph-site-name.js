import {makeSuite, makeCase} from 'ginny';

export default makeSuite('Тег meta og:site_name', {
    feature: 'SEO',
    story: {
        'По умолчанию': {
            'должен соответствовать ожидаемому.': makeCase({
                test() {
                    return this.base
                        .getOpenGraphSiteNameContent()
                        .should.eventually.to.equal(
                            'Яндекс.Маркет',
                            'Атрибут content равен Яндекс.Маркет'
                        );
                },
            }),
        },
    },
});
