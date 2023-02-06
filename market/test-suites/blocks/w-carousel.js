import {makeSuite, makeCase} from 'ginny';

/**
 * Тесты на виджет Carousel
 * @param {PageObject.Carousel} carousel
 */
export default makeSuite('Carousel', {
    id: 'm-touch-2530',
    issue: 'MOBMARKET-10967',
    story: {
        'Ссылка у сниппета вендора.': {
            'содержит слаг': makeCase({
                test() {
                    return this.carousel.getVendorHref()
                        .should.eventually.be.link({
                            pathname: 'brands--[\\w-]+',
                        }, {
                            mode: 'match',
                            skipProtocol: true,
                            skipHostname: true,
                        });
                },
            }),
        },
    },
});
