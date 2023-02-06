import {makeSuite, makeCase} from 'ginny';

export default makeSuite('Тег Open Graph image', {
    feature: 'SEO',
    story: {
        'По умолчанию': {
            'не должен быть пустым.': makeCase({
                id: 'm-touch-1041',
                issue: 'MOBMARKET-6319',
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
