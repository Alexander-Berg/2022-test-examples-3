describe('UserPic', function() {
    hermione.skip.in(['win-ie11', 'linux-chrome', 'linux-firefox'], 'https://st.yandex-team.ru/FEI-19180', { silent: true });
    it('static', function() {
        return this.browser
            .url('UserPic/hermione/hermione.html')
            // Сдвигаем курсор, т.к. в ie11 курсор первоначально находится на компоненте,
            // из-за этого возникает hovered эффект.
            .moveToObject('body', 500, 500)
            .assertView('plain', ['body']);
    });

    it('hasCamera', function() {
        return this.browser
            .url('UserPic/hermione/hermione.html')
            .moveToObject('.UserPic_size_m .UserPic-Camera')
            .assertView('hovered', ['.UserPic_size_m .UserPic-Camera']);
    });
});
