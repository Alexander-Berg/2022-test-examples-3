specs({
    feature: 'newsVideoBlock',
}, () => {
    hermione.only.notIn('safari13');
    it('Внешний вид блока', function() {
        const selector = '.news-video-block';
        return this.browser
            .url('/turbo?stub=newsvideoblock/default.json')
            .assertView('plain', selector);
    });
});
