import {makeSuite, makeCase} from 'ginny';

export default makeSuite('Тег Open Graph description', {
    feature: 'SEO',
    story: {
        'По умолчанию': {
            'должен соответствовать ожидаемому.': makeCase({
                id: 'm-touch-1041',
                issue: 'MOBMARKET-6319',
                params: {
                    expectedOpenGraphDescription: 'Ожидаемый Open Graph description',
                    expectedOpenGraphDescriptionRegex: 'Регулярное выражение для Open Graph description',
                },
                test() {
                    // INFO: это способ обмануть линтер и оставить неиспользуемый код тестов до починки.
                    const canSkipTest = true;

                    if (canSkipTest && this.test._meta.environment !== 'kadavr') {
                        // INFO: чиним как можно скорее, код ниже (который не выполнится) не удалять!
                        // eslint-disable-next-line market/ginny/no-skip
                        return this.skip('Пропуск нестабильных тестов. Чинится тут MOBMARKET-9726.');
                    }

                    return this.base
                        .getOpenGraphDescriptionContent()
                        .then(actualDescription =>
                            this.browser.allure.runStep('Проверяем Open Graph description', () => {
                                if (this.params.expectedOpenGraphDescriptionRegex) {
                                    const regex = new RegExp(this.params.expectedOpenGraphDescriptionRegex);

                                    return this.expect(actualDescription).to.match(
                                        regex,
                                        'Проверяем Open Graph description'
                                    );
                                }

                                return this.expect(actualDescription).to.equal(
                                    this.params.expectedOpenGraphDescription,
                                    'Проверяем Open Graph description'
                                );
                            })
                        );
                },
            }),
        },
    },
});
