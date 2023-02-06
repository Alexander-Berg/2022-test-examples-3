specs({
    feature: 'newsVideoCard',
}, () => {
    it('Внешний вид блока', function() {
        return this.browser
            .url('/turbo?stub=newsvideocard/default.json')
            .yaWaitForVisible('.hermione__sport-video-card', 'Блок не появился')
            .assertView('sport', '.hermione__sport-video-card');
    });
});
