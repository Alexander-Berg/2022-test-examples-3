import {makeSuite, makeCase} from 'ginny';

/**
 * Тесты на блок Linker.
 * @param {PageObject.Linker} linker
 */
export default makeSuite('Блок перелинковки. Кол-во ссылок.', {
    feature: 'Блок перелинковки',
    story: {
        'По умолчанию': {
            'ссылок не должно быть больше заданового кол-ва': makeCase({
                id: 'marketfront-2870',
                issue: 'MARKETVERSTKA-29644',
                params: {
                    counts: 'Кол-во рецептов',
                },
                defaultParams: {
                    counts: 15,
                },
                test() {
                    return this.linker.getLinksLength()
                        .should.eventually.to.be.lte(this.params.counts, `Кол-во ссылок меньше ${this.params.counts}`);
                },
            }),
        },
    },
});
