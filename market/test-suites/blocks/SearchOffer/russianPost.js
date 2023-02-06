import {makeCase, makeSuite} from 'ginny';


/**
 * @param {PageObject.SearchOffer} snippet
 */
export default makeSuite('Дисклеймер о Почте России.', {
    environment: 'kadavr',
    story: {
        'По умолчанию': {
            'должен присутствовать': makeCase({
                test() {
                    return this.snippetDelivery.getText().should.eventually.to.equal(
                        'Почтой России', 'Дисклеймер о Почте России должен присутствовать'
                    );
                },
            }),
        },
    },
});
