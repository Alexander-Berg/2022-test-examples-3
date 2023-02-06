import {makeSuite, makeCase} from 'ginny';

export default makeSuite('Тег meta og:image', {
    feature: 'SEO',
    story: {
        'По умолчанию': {
            'не должен быть пустым.': makeCase({
                test() {
                    return this.base
                        .getOpenGraphImageContent()
                        .then(image => this.expect(Boolean(image))
                            .to.equal(true, `Проверяем, что картинка «${image}» не пустая`)
                        );
                },
            }),
        },
    },
});
