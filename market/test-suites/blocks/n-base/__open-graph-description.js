import {makeSuite, makeCase} from 'ginny';

export default makeSuite('Тег meta og:description', {
    feature: 'SEO',
    story: {
        'По умолчанию': {
            'должен соответствовать ожидаемому.': makeCase({
                params: {
                    expectedDescription: 'Ожидаемый Open Graph description',
                    expectedDescriptionRegex: 'Регулярное выражение для Open Graph description',
                },
                test() {
                    return this.base
                        .getOpenGraphDescriptionContent()
                        .then(actualDescription => {
                            if (this.params.expectedDescriptionRegex) {
                                const regex = new RegExp(this.params.expectedDescriptionRegex);

                                return this.expect(actualDescription).to.match(
                                    regex,
                                    'Атрибут content соответствует переданному в параметрах'
                                );
                            }

                            return this.expect(actualDescription).to.equal(
                                this.params.expectedDescription,
                                'Атрибут content равен переданному в параметрах'
                            );
                        });
                },
            }),
        },
    },
});
