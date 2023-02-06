specs({
    feature: 'Новостной медиа-блок на десктопе',
}, () => {
    const selector = '.news-media-stack';

    it('Базовый вид блока - фото и видео', function() {
        return this.browser
            .url('/turbo?stub=newsmediastack/default.json')
            .assertView('plain', selector, { ignoreElements: PO.embed() });
    });

    it('Базовый вид блока - фото', function() {
        return this.browser
            .url('/turbo?stub=newsmediastack/photo.json')
            .assertView('plain-photo', selector);
    });
});
