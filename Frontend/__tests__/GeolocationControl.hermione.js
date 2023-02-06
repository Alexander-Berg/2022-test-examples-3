specs({
    feature: 'geolocationControl',
}, () => {
    hermione.skip.in(/firefox/, 'Там не удаётся подменить Geolocation API');
    hermione.only.notIn('safari13');
    it('Внешний вид блока', function() {
        this.browser.yaAssertRegionView || this.browser.addCommand('yaAssertRegionView', yaAssertRegionView);

        return this.browser
            .url('/turbo?stub=geolocationcontrol/default.json')
            .execute(function() {
                window.navigator.geolocation = {
                    getCurrentPosition: function(successCallback) {
                        successCallback({
                            timestamp: Date.now(),
                            coords: {
                                latitude: 55.55555555,
                                longitude: 37.37373737,
                                accuracy: 1000,
                            },
                        });
                    },
                };
            })
            .yaWaitForVisible(PO.geolocationControl())
            .assertView('current', PO.geolocationControl())
            .click(PO.geolocationControl.clarify())
            .waitForVisible(PO.geolocationControl.visibleErrorPopup())
            .pause(300) // Длительность анимации появления попапа

            // Снимаем не сам попап, а просто верхние 65px страницы: у попапа есть тени, поэтому
            // его bounding box выходит за пределы вьюпорта и assertView отказывается его снимать
            .yaAssertRegionView('error-popup', 'top: 0; left: 0; right: 0; height: 65px;')
            .click(PO.geolocationControl.errorPopupClose())
            .assertView('plain', PO.geolocationControl());
    });
});

function yaAssertRegionView(state, position) {
    return this
        .execute(function(pos) {
            var elem = document.createElement('div');
            elem.id = 'assert-region-view-mock';
            elem.setAttribute('style', 'position: fixed; ' + pos);
            document.body.appendChild(elem);
        }, position)
        .assertView(state, '#assert-region-view-mock')
        .execute(function() {
            var elem = document.getElementById('assert-region-view-mock');
            elem.parentNode.removeChild(elem);
        });
}
