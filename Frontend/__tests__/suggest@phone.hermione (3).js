specs({
    feature: 'suggest',
}, () => {
    hermione.only.notIn('safari13');
    it('Базовый вид блока', function() {
        return this.browser
            .url('/turbo?stub=suggest/default.json')
            .assertView('plain', PO.page())
            .yaMockXHR({
                recordData: ['suggest-endings'],
                urlDataMap: {
                    '&part=&': ['', '', [], { suggestions: [] }],
                    '&part=%D1%81%D1%82%D0%BE%D0%BB%D0%B8%D1%86%D0%B0%20%D1%81%D1%88': ['столица сша', 'столица сша википедия', [['столица сша стаб дата', 0, { tpah: [12, 12, 21] }], ['столица сша нью-йорк', 0, { tpah: [12, 12, 20] }], ['столица сша название', 0, { tpah: [12, 12, 20] }], ['столица сша это', 0, { tpah: [12, 12, 15] }], ['столица сша на карте', 0, { tpah: [12, 12, 20] }], ['столица сша на английском', 0, { tpah: [12, 12, 25] }]], ['fact', 'столица США название', 'Вашингтон'], {}],
                    '&part=music.yandex': ['music.yandex', 'music.yandex.ru', [['music.yandex.ru', 0, { tpah: [0, 12, 15] }], ['music.yandex.ru\/gift', 0, { tpah: [0, 12, 20] }], ['music.yandex.ua', 0, { tpah: [0, 12, 15] }], ['music.yandex.by', 0, { tpah: [0, 12, 15] }], ['music.yandex.kz', 0, { tpah: [0, 12, 15] }], ['music.yandex yandex music', 0, { tpah: [13, 13, 25] }]], ['nav', 'Яндекс.Музыка — поиск музыки', 'Яндекс.Музыка — поиск музыки', 'music.yandex.ru', 'http:\/\/yandex.ru\/clck\/jsredir?from=yandex.ru%3Bsuggest%3Bweb&text=&etext=2000.nCeRh-71wbHZu5Z5huVfdpu3zBiH9hhByD2g2pUWUp1JOtWH9Ej0o3jy_4Jm66GJ.ec7fa9bd33bea6729d4c67cade06e58502a57329&uuid=&state=_lAfmgRBPT3cYiVJtCsp2DXv51ffktCe3DxjgLDCbTxJQEcfl2rx1ii7Ee5fjzIVxa2X78PqhQFVrUJrnvjG8niSmiOZ9XWE-1KDXivUTC2QGesaXgt3s4N0os_gWBSN&data=d19IS0g3U2hIb3JjVXZiNmREU2NPeUZGUG9iWHZTTlU4bjhYQUMyVmhtM3VlcExrLU9hQjNmNjNfbWhBallCTU9wcnhwRUttOE84LA,,&sign=11978aa5ef78d259870cfa69ed53d880&keyno=16&b64e=2&ref=kDmUwfHjWH9QRbbmW23nC3ay3Tz0MS3nAoDfD7bRO8Rcm1X2oLVjua_SXY018nfo', { type: 'base-rca', img: { url: 'https:\/\/avatars.mds.yandex.net\/get-shinyserp\/1383221\/2a0000016adb89e86baaf34084f25cd92fcd\/square_96', aspect: 'square', contain: true }, mark: ['verified'] }], {}],
                },
            })
            .yaWaitForVisible(PO.suggest(), 'Блок suggest не появился на странице')
            .click(PO.suggest.miniSuggest.opener())
            .assertView('focused', PO.page())
            .keys('столица сша')
            .yaWaitForVisible(PO.suggest.miniSuggest.popup(), 'Попап саджеста не всплыл после ввода фразы')
            .assertView('washington', PO.suggest.miniSuggest.itemFact())
            .click(PO.suggest.miniSuggest.inputClear())
            .assertView('focused-again', PO.page())
            .keys('music.yandex')
            .yaWaitForVisible(PO.suggest.miniSuggest.popup(), 'Попап саджеста не всплыл после ввода фразы')
            .yaMockImages()
            .assertView('yandex', PO.suggest.miniSuggest.itemNav(), {
                screenshotDelay: 2000, // для того, чтобы скроллбар успел скрыться
            });
    });
});
