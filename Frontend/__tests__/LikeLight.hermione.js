specs({
    feature: 'LikeLight',
}, () => {
    describe('Число лайков = 0', () => {
        hermione.only.notIn('safari13');
        it('Статус none', function() {
            return this.browser
                .url('?stub=likelight/default.json')
                .yaWaitForVisible(PO.likeLight())
                .assertView('default-none', PO.likeLight());
        });

        hermione.only.notIn('safari13');
        it('Статус liked', function() {
            return this.browser
                .url('?stub=likelight/default-liked.json')
                .yaWaitForVisible(PO.likeLight())
                .assertView('default-liked', PO.likeLight());
        });

        hermione.only.notIn('safari13');
        it('Статус disliked', function() {
            return this.browser
                .url('?stub=likelight/default-disliked.json')
                .yaWaitForVisible(PO.likeLight())
                .assertView('default-disliked', PO.likeLight());
        });
    });

    describe('Число лайков = 100', () => {
        hermione.only.notIn('safari13');
        it('Статус none', function() {
            return this.browser
                .url('?stub=likelight/positive.json')
                .yaWaitForVisible(PO.likeLight())
                .assertView('positive', PO.likeLight());
        });

        hermione.only.notIn('safari13');
        it('Статус liked', function() {
            return this.browser
                .url('?stub=likelight/positive-liked.json')
                .yaWaitForVisible(PO.likeLight())
                .assertView('positive-liked', PO.likeLight());
        });

        hermione.only.notIn('safari13');
        it('Статус disliked', function() {
            return this.browser
                .url('?stub=likelight/positive-disliked.json')
                .yaWaitForVisible(PO.likeLight())
                .assertView('positive-disliked', PO.likeLight());
        });
    });

    describe('Число лайков = -100', () => {
        hermione.only.notIn('safari13');
        it('Статус none', function() {
            return this.browser
                .url('?stub=likelight/negative.json')
                .yaWaitForVisible(PO.likeLight())
                .assertView('negative', PO.likeLight());
        });

        hermione.only.notIn('safari13');
        it('Статус liked', function() {
            return this.browser
                .url('?stub=likelight/negative-liked.json')
                .yaWaitForVisible(PO.likeLight())
                .assertView('negative-liked', PO.likeLight());
        });

        hermione.only.notIn('safari13');
        it('Статус disliked', function() {
            return this.browser
                .url('?stub=likelight/negative-disliked.json')
                .yaWaitForVisible(PO.likeLight())
                .assertView('negative-disliked', PO.likeLight());
        });
    });
});
