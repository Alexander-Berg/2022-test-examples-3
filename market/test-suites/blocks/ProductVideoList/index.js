import {makeCase, makeSuite} from 'ginny';

/**
 * @param {PageObject.ProductVideoList} productVideoList
 */
export default makeSuite('Список видео', {
    story: {
        'По умолчанию': {
            'отображается.': makeCase({
                id: 'm-touch-2962',
                issue: 'MOBMARKET-13174',
                test() {
                    return this.productVideoList.isVisible()
                        .should.eventually.to.be.equal(true, 'Виджет отображается');
                },
            }),
        },
    },
});
