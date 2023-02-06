import {makeSuite, makeCase} from 'ginny';

export default makeSuite('ScrollBox ссылки.', {
    feature: 'ScrollBox',
    environment: 'testing',
    story: {
        Ссылки: {
            'должны удовлетворять регулярке.': makeCase({
                id: 'm-touch-1934',
                async test() {
                    const links = await this.ScrollBox.getLinksHref();
                    return this.expect(links.every(link => link.match(this.params.pathname)))
                        .to.be.equal(true, 'Проверяем ссылки на соответствие регулярному выражению.');
                },
            }),
        },
    },
});
