'use strict';

const PO = require('../../page-objects/touch-phone/index').PO;

specs({
    feature: 'Турбо сниппет'
}, function() {
    const path = '/$page/$main/$result';

    it('По заголовку', function() {
        return this.browser
            .yaOpenSerp({
                text: 'Кот Шрёдингера'
            }, PO.turboSnippet())
            .yaCheckBaobabServerCounter({
                path: '/$page/$main/$result/title[@turbo=true]'
            })
            .yaCheckBaobabServerCounter({
                path: '/$page/$main/$result/path/urlnav[@turbo=true]'
            })
            .yaCheckBaobabCounter(PO.turboSnippet.title.link(), {
                path: `${path}/title[@turbo=true]`
            }, 'Не сработал счетчик в тайтле').then(dataset =>
                this.browser.yaCheckURL(
                    dataset[0].url,
                    { pathname: '/turbo' },
                    { skipProtocol: true, skipHostname: true, skipQuery: true }
                )
            )
            .yaCheckBaobabCounter(PO.turboSnippet.path(), {
                path: `${path}/path/urlnav[@turbo=true]`
            }, 'Не сработал счетчик в гринурле').then(dataset =>
                this.browser.yaCheckURL(
                    dataset[0].url,
                    { pathname: '/turbo' },
                    { skipProtocol: true, skipHostname: true, skipQuery: true }
                )
            );
    });

    it('По ссылке', function() {
        return this.browser
            .yaOpenSerp({
                foreverdata: 2422376836
            }, PO.turboSnippet())
            .yaCheckBaobabCounter(PO.turboSnippet.linkHandler(), {
                path: `${path}/turbo_link`
            }, 'Не сработал счетчик в ссылке "Турбо-страница"').then(dataset =>
                this.browser.yaCheckURL(
                    dataset[0].url,
                    { pathname: '/turbo' },
                    { skipProtocol: true, skipHostname: true, skipQuery: true }
                )
            );
    });

    it('Ссылка с якорем', function() {
        return this.browser
            .yaOpenSerp({
                foreverdata: 4252077949
            }, PO.turboSnippet())
            .yaCheckLink(PO.turboSnippet.title.link()).then(url => {
                const href = url.href.replace(/parent-reqid=([^&#]+)/, 'parent-reqid=XXX');

                assert.equal(url.hash, '#anchor-1');
                assert.equal(href, 'https://yandex.ru/turbo?text=bin%3Apage-with-id-attrs&parent-reqid=XXX&lite=1#anchor-1');
            });
    });
});
