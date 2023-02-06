specs({
    feature: 'Like',
}, () => {
    describe('Число лайков = 0', () => {
        hermione.only.notIn('safari13');
        it('Статус none', function() {
            return this.browser
                .url('?stub=like/default.json')
                .yaWaitForVisible(PO.like())
                .assertView('default', PO.like());
        });

        hermione.only.notIn('safari13');
        it('Статус liked', function() {
            return this.browser
                .url('?stub=like/default-liked.json')
                .yaWaitForVisible(PO.like())
                .assertView('default-liked', PO.like());
        });

        hermione.only.notIn('safari13');
        it('Статус disliked', function() {
            return this.browser
                .url('?stub=like/default-disliked.json')
                .yaWaitForVisible(PO.like())
                .assertView('default-disliked', PO.like());
        });
    });

    describe('Число лайков = 1234', () => {
        hermione.only.notIn('safari13');
        it('Статус none', function() {
            return this.browser
                .url('?stub=like/positive.json')
                .yaWaitForVisible(PO.like())
                .assertView('positive', PO.like());
        });

        hermione.only.notIn('safari13');
        it('Статус liked', function() {
            return this.browser
                .url('?stub=like/positive-liked.json')
                .yaWaitForVisible(PO.like())
                .assertView('positive-liked', PO.like());
        });

        hermione.only.notIn('safari13');
        it('Статус disliked', function() {
            return this.browser
                .url('?stub=like/positive-disliked.json')
                .yaWaitForVisible(PO.like())
                .assertView('positive-disliked', PO.like());
        });
    });

    describe('Число лайков = -1234', () => {
        hermione.only.notIn('safari13');
        it('Статус none', function() {
            return this.browser
                .url('?stub=like/negative.json')
                .yaWaitForVisible(PO.like())
                .assertView('negative', PO.like());
        });

        hermione.only.notIn('safari13');
        it('Статус liked', function() {
            return this.browser
                .url('?stub=like/negative-liked.json')
                .yaWaitForVisible(PO.like())
                .assertView('negative-liked', PO.like());
        });

        hermione.only.notIn('safari13');
        it('Статус disliked', function() {
            return this.browser
                .url('?stub=like/negative-disliked.json')
                .yaWaitForVisible(PO.like())
                .assertView('negative-disliked', PO.like());
        });
    });
});
