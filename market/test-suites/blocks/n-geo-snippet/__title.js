import {makeCase, makeSuite} from 'ginny';

/**
 * @param {PageObject.GeoSnippet} geoSnippet
 */
export default makeSuite('Заголовок сниппета на карте', {
    story: {
        'По умолчанию': {
            'виден на странице': makeCase({
                async test() {
                    const visible = this.geoSnippet.title.isVisible();

                    await this.expect(visible).to.be.equal(true, 'Заголовка нет на странице');
                },
            }),
        },
    },
});
