import {makeSuite, makeCase} from 'ginny';

/**
 * Тесты на виджет BubbleNavigation
 * @param {PageObject.BubbleNavigation} bubbleNavigation
 */
export default makeSuite('BubbleNavigation', {
    id: 'm-touch-2530',
    issue: 'MOBMARKET-11605',
    story: {
        'Ссылка у сниппета вендора.': {
            'содержит слаг': makeCase({
                test() {
                    return this.bubbleNavigation.getVendorHref()
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
