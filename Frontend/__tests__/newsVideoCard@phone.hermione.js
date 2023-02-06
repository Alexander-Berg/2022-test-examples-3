specs({
    feature: 'newsVideoCard',
}, () => {
    hermione.only.notIn('safari13');
    it('Внешний вид блока', function() {
        return this.browser
            .url('/turbo?stub=newsvideocard/default.json')
            .yaWaitForVisible('.hermione__sport-video-card', 'Блок не появился')
            .assertView('plain', '.hermione__sport-video-card');
    });
});
