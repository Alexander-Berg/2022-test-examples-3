'use strict';

const PO = require('../../page-objects/touch-phone/index').PO;

specs({
    feature: 'БНО',
    type: 'с приложением'
}, function() {
    it('Проверка ссылок и счетчиков', function() {
        const sitelinkCounter = {
            path: '/$page/$main/$result/sitelinks/link'
        };

        return this.browser
            .yaOpenSerp('text=вконтакте')
            .yaWaitForVisible(PO.bno())
            .yaCheckSnippet(PO.bno, {
                title: {
                    baobab: {
                        path: '/$page/$main/$result/title'
                    }
                },
                greenurl: [{
                    baobab: {
                        path: '/$page/$main/$result/path/urlnav'
                    }
                }],
                sitelinks: [{
                    selector: PO.bno.firstLink(),
                    baobab: sitelinkCounter
                }, {
                    selector: PO.bno.secondLink(),
                    baobab: sitelinkCounter
                }, {
                    selector: PO.bno.thirdLink(),
                    baobab: sitelinkCounter
                }, {
                    selector: PO.bno.fourthLink(),
                    baobab: sitelinkCounter
                }]
            })
            .yaCheckLink(PO.bno.app.button())
            .yaCheckBaobabCounter(PO.bno.app.button(), {
                path: '/$page/$main/$result/bna_app/button'
            });
    });

    it('Проверка наличия турбо лэйбла', function() {
        const path = '/$page/$main/$result';

        return this.browser.yaOpenSerp('text=новая газета')
            .yaWaitForVisible(PO.bno())
            .assertView('plain', PO.bno())
            .yaCheckBaobabServerCounter({
                path: '/$page/$main/$result/title[@turbo=true]'
            })
            .yaCheckBaobabServerCounter({
                path: '/$page/$main/$result/path/urlnav[@turbo=true]'
            })
            .yaCheckBaobabCounter(PO.bno.title.link(), {
                path: `${path}/title[@turbo=true]`
            }, 'Не сработал счетчик в тайтле').then(dataset =>
                this.browser.yaCheckURL(
                    dataset[0].url,
                    { pathname: '/turbo' },
                    { skipProtocol: true, skipHostname: true, skipQuery: true }
                )
            )
            .yaCheckBaobabCounter(PO.bno.path(), {
                path: `${path}/path/urlnav[@turbo=true]`
            }, 'Не сработал счетчик в гринурле').then(dataset =>
                this.browser.yaCheckURL(
                    dataset[0].url,
                    { pathname: '/turbo' },
                    { skipProtocol: true, skipHostname: true, skipQuery: true }
                )
            );
    });
});
