specs({
    feature: 'Спортивные видео',
}, () => {
    hermione.only.notIn('safari13');
    it('Карусель', function() {
        const selector = '.sport-video-list';

        return this.browser
            .url('/turbo?stub=sportvideolist/carousel.json')
            .yaWaitForVisible(selector, 'Блок не появился')
            .assertView('carousel', selector);
    });

    hermione.only.notIn('safari13');
    it('Одно видео', function() {
        const selector = '.sport-video-list';

        return this.browser
            .url('/turbo?stub=sportvideolist/single.json')
            .yaWaitForVisible(selector, 'Блок не появился')
            .assertView('single', selector);
    });

    hermione.only.notIn('safari13');
    it('Одно видео с коротким названием', function() {
        const selector = '.sport-video-list';

        return this.browser
            .url('/turbo?stub=sportvideolist/single-short-title.json')
            .yaWaitForVisible(selector, 'Блок не появился')
            .assertView('single-short-title', selector);
    });
});
