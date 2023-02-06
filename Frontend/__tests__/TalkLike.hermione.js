specs({
    feature: 'TalkLike',
}, () => {
    hermione.only.notIn('safari13');
    it('Отображение спиннера', function() {
        return this.browser
            .url('?stub=talklike/example.json')
            .yaWaitForVisible(PO.talkLikeNotLoading())
            .execute(talkLikeSelector => {
                document.querySelector(talkLikeSelector).classList.add('talk-like_loading');
            }, PO.talkLike())
            .assertView('spinner', PO.talkLike());
    });

    hermione.only.notIn('safari13');
    it('Отображение блока like', function() {
        return this.browser
            .url('?stub=talklike/example.json')
            .yaWaitForVisible(PO.talkLike())
            .execute(talkLikeSelector => {
                document.querySelector(talkLikeSelector).classList.remove('talk-like_loading');
            }, PO.talkLike())
            .assertView('like', PO.talkLike());
    });

    hermione.only.notIn('safari13');
    it('Выставление лайка', function() {
        return this.browser
            .url('?stub=talklike/example.json')
            .yaWaitForVisible(PO.talkLike())
            .execute(talkLikeSelector => {
                document.querySelector(talkLikeSelector).classList.remove('talk-like_loading');
            }, PO.talkLike())
            .yaWaitForVisible(PO.like.button())
            .click(PO.like.button())
            .yaWaitForVisible(PO.like.buttonPressed())
            .assertView('liked', PO.talkLike());
    });

    hermione.only.notIn('safari13');
    it('Выставление дизлайка', function() {
        return this.browser
            .url('?stub=talklike/example.json')
            .yaWaitForVisible(PO.talkLike())
            .execute(talkLikeSelector => {
                document.querySelector(talkLikeSelector).classList.remove('talk-like_loading');
            }, PO.talkLike())
            .yaWaitForVisible(PO.like.buttonDislike())
            .click(PO.like.buttonDislike())
            .yaWaitForVisible(PO.like.buttonPressed())
            .assertView('disliked', PO.talkLike());
    });
});
