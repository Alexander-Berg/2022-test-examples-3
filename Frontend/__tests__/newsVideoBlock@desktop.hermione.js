specs({
    feature: 'newsVideoBlock',
}, () => {
    it('Внешний вид блока', function() {
        const selector = '.news-video-block';
        return this.browser
            .url('/turbo?stub=newsvideoblock/default.json')
            .windowHandleSize({ width: 1000, height: 1500 })
            .assertView('plain-1000', selector)
            .windowHandleSize({ width: 1280, height: 1500 })
            .assertView('plain-1280', selector)
            .windowHandleSize({ width: 1366, height: 1500 })
            .assertView('plain-1366', selector)
            .windowHandleSize({ width: 1600, height: 1500 })
            .assertView('plain-1600', selector)
            .windowHandleSize({ width: 1700, height: 1500 })
            .assertView('plain-1700', selector);
    });
});
