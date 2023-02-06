import {makeSuite, makeCase} from 'ginny';

/**
 * @param {PageObject.GeoSnippet} geoSnippet
   @param {PageObject.GeoBlockPanelSnippets} geoPanelSnippets
 */
export default makeSuite('Сниппет.', {
    feature: 'Подтверждение возраста',
    story: {
        async beforeEach() {
            return this.geoSnippet.waitForVisible();
        },
        'содержит дисклеймер "Возрастное ограничение" и иконку 18+': makeCase({
            id: 'marketfront-826',
            issue: 'MARKETVERSTKA-24984',
            async test() {
                return this.productWarnings.isVisible()
                    .should.eventually.be.equal(true, 'Возрастное предупреждение должно быть показано');
            },
        }),
    },
});
