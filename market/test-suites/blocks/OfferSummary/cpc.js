import {makeSuite, makeCase} from 'ginny';

export default makeSuite('Информация об оффере', {
    story: {
        'Кликаут': {
            'Кнопка "В магазин"': {
                'Имеет корректную ссылку': makeCase({
                    async test() {
                        const url = await this.offerSummary.getClickoutButtonUrl();
                        return this.expect(url.path)
                            .to.equal(this.params.url, 'Кликаут ссылка корректна');
                    },
                }),
            },
        },
    },
});
