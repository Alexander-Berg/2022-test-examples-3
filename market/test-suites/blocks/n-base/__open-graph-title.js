import {makeSuite, makeCase} from 'ginny';

export default makeSuite('Тег meta og:title', {
    feature: 'SEO',
    story: {
        'По умолчанию': {
            'должен соответствовать ожидаемому.': makeCase({
                params: {
                    expectedTitle: 'Ожидаемый Open Graph title',
                    expectedTitleRegex: 'Регулярное выражение для тайтла',
                },
                test() {
                    return this.base
                        .getOpenGraphTitleContent()
                        .then(actualTitle => {
                            if (this.params.expectedTitleRegex) {
                                const regex = new RegExp(this.params.expectedTitleRegex);

                                return this.expect(actualTitle).to.match(
                                    regex,
                                    'Атрибут content соответствует переданному в параметрах'
                                );
                            }

                            return this.expect(actualTitle).to.equal(
                                this.params.expectedTitle,
                                'Атрибут content равен переданному в параметрах'
                            );
                        });
                },
            }),
        },
    },
});
