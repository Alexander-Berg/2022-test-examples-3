import {makeSuite, makeCase} from 'ginny';

/**
 * Тесты на блок Linker.
 * @param {PageObject.Linker} linker
 */
export default makeSuite('Блок перелинковки. Видимость.', {
    feature: 'Блок перелинковки',
    params: {
        visibleLinks: 'Кол-во ссылок',
    },
    story: {
        'По умолчанию': {
            'блок должен присутствовать на странице': makeCase({
                id: 'marketfront-2869',
                issue: 'MARKETVERSTKA-29644',
                async test() {
                    await this.linker.isExisting()
                        .should.eventually.to.be.equal(true, 'Блок присутствует на странице');
                },
            }),
        },
    },
});
