import {makeSuite, makeCase} from 'ginny';

/**
 * Тесты на виджет LogoCarousel
 * @param {PageObject.LogoCarousel} logoCarousel
 */
export default makeSuite('LogoCarousel', {
    id: 'm-touch-2530',
    issue: 'MOBMARKET-10967',
    story: {
        'Ссылка у сниппета вендора.': {
            'содержит слаг': makeCase({
                test() {
                    return this.logoCarousel.getBannerHref()
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
