specs({
    feature: 'LcFlashlight',
}, function() {
    describe('Фонарик', function() {
        hermione.only.notIn('safari13');
        it('Фонарик работает с картинкой', function() {
            return this.browser
                .url('/turbo?stub=lcflashlight/default.json')
                .yaWaitForVisible(PO.lcFlashlight(), 'Фонарик не появился')
                .moveToObject(PO.lcFlashlight(), 256, 256)
                .pause(1000)
                .assertView('flashlight', PO.lcFlashlight());
        });

        hermione.only.notIn('safari13');
        it('Фонарик работает в слоёной группе', function() {
            return this.browser
                .url('/turbo?stub=lcflashlight/with-layers-group.json')
                .yaWaitForVisible(PO.lcFlashlight(), 'Фонарик не появился')
                .moveToObject(PO.lcFlashlight(), 256, 256)
                .pause(1000)
                .assertView('flashlight', PO.lcFlashlight());
        });
    });
});
