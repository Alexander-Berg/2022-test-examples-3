const { checkReactionSetting, checkScrollToComments, checkBlockView } = require('./helper.hermione');

hermione.skip.in(/.*/, 'https://st.yandex-team.ru/TURBOUI-2136');
specs({
    feature: 'TalkSocialPanel',
}, () => {
    hermione.only.notIn('safari13');
    it('Отображение блока', function() {
        return this.browser
            .url('?stub=talksocialpanel/page.json')
            .then(checkBlockView.bind(this));
    });

    hermione.only.notIn('safari13');
    it('Выставление лайка', function() {
        return this.browser
            .url('?stub=talksocialpanel/page.json')
            .then(checkReactionSetting.bind(this, PO.likeLight.iconLike(), 'liked'));
    });

    hermione.only.notIn('safari13');
    it('Выставление дизлайка', function() {
        return this.browser
            .url('?stub=talksocialpanel/page.json')
            .then(checkReactionSetting.bind(this, PO.likeLight.iconDislike(), 'disliked'));
    });

    hermione.only.notIn('safari13');
    it('Прокрутка к блоку комментариев', function() {
        return this.browser
            .url('?stub=talksocialpanel/page.json')
            .then(checkScrollToComments.bind(this));
    });
});
