import {makeSuite, makeCase} from 'ginny';

export default makeSuite('Блок с комплектом со скидкой.', {
    feature: 'Блок с комплектом со скидкой.',
    id: 'bluemarket-3613',
    story: {
        'Отображается блок с комплектом.': makeCase({
            async test() {
                await this.offerSet.isVisible()
                    .should.eventually.be.equal(true, 'Блок с комплектом должен быть виден');
            },
        }),
    },
});
