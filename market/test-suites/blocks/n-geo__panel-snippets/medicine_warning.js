import {makeSuite, makeCase} from 'ginny';

/**
 * @param {PageObject.GeoSnippet} geoSnippet
 * @param {PageObject.ProductWarinings} productWarnings
 */
export default makeSuite('Дисклеймер.', {
    feature: 'Дисклеймеры',
    story: {
        'По умолчанию': {
            async beforeEach() {
                return this.geoSnippet.waitForVisible();
            },
            'должен отображаться': makeCase({
                async test() {
                    return this.productWarnings.getMedicineWarning().isVisible()
                        .should.eventually.equal(true, 'Дисклеймер присутствует');
                },
            }),
        },
    },
});
