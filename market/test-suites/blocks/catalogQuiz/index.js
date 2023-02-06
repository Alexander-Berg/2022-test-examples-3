import {makeCase, makeSuite} from 'ginny';

export default makeSuite('Подборщик', {
    environment: 'kadavr',
    issue: 'MARKETFRONT-71755',
    story: {
        'Отображается по hid': makeCase({
            id: 'marketfront-5572',
            async test() {
                await this.browser.yaOpenPage(this.params.pageId, {
                    'local-offers-first': 0,
                    nid: 192954,
                    hid: 67890,
                    slug: 'ololo',
                    onstock: 1,
                    'rearr-factors': 'market_cms_podborshik',
                });
                await this.StartScreen.isExisting()
                    .should.eventually.to.be.equal(
                        true,
                        'Подборщик должен отображаться'
                    );
            },
        }),
        'Отображается по nid': makeCase({
            id: 'marketfront-5571',
            async test() {
                await this.browser.yaOpenPage(this.params.pageId, {
                    'local-offers-first': 0,
                    nid: 192954,
                    slug: 'ololo',
                    onstock: 1,
                    'rearr-factors': 'market_cms_podborshik',
                });
                await this.StartScreen.isExisting()
                    .should.eventually.to.be.equal(
                        true,
                        'Подборщик должен отображаться'
                    );
            },
        }),
    },
});
