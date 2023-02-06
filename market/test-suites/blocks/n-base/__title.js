import {makeSuite, makeCase} from 'ginny';

export default makeSuite('Тег title', {
    feature: 'SEO',
    story: {
        'По умолчанию': {
            'должен соответствовать ожидаемому.': makeCase({
                params: {
                    expectedTitle: 'Ожидаемый тайтл страницы',
                    expectedTitleRegex: 'Регулярное выражение для тайтла',
                },
                test() {
                    return this.base
                        .getTitleContent()
                        .then(actualTitle => {
                            if (this.params.expectedTitleRegex) {
                                const regex = new RegExp(this.params.expectedTitleRegex);

                                return this.expect(actualTitle).to.match(
                                    regex,
                                    'Содержимое тега соответствует переданному в параметрах'
                                );
                            }

                            return this.expect(actualTitle).to.equal(
                                this.params.expectedTitle,
                                'Содержимое тега равно переданному в параметрах'
                            );
                        });
                },
            }),
        },
    },
});
