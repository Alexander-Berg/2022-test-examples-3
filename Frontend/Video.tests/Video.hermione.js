specs({
    feature: 'VideoPlayer',
    experiment: 'Видео-плеер на Реакте',
}, () => {
    hermione.only.notIn('safari13');
    it('Основные проверки', function() {
        return this.browser
            .url('/turbo?stub=video/default.json&exp_flags=video-player-react=1')
            .yaWaitForVisible(PO.blocks.video(), 'Видео-плеер не появился')
            .assertView('plain', PO.blocks.video());
    });

    hermione.only.notIn('safari13');
    it('С описанием', function() {
        return this.browser
            .url('/turbo?stub=video/with-caption.json&exp_flags=video-player-react=1')
            .yaWaitForVisible(PO.blocks.video(), 'Видео-плеер не появился')
            .yaWaitForVisible(PO.blocks.media.content(), 'Описание не появилось')
            .assertView('plain-with-caption', PO.blocks.media());
    });
});
