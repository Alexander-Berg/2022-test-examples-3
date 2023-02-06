const defaultCheckPageConf = { skipProtocol: true, skipHostname: true, skipQuery: true };

describe('Внутренняя страница 404', () => {
    describe('Авторизация на странице 404', () => {
        hermione.skip.in([/linux-firefox/], 'протухший tls сертификат');
        it('Пользователь залогинен', function() {
            return this.browser
                .yaLogin()
                .yaOpenPage('/ege/')
                .yaCheckPageUrl('/tutor/ege/', defaultCheckPageConf)
                .yaShouldBeVisible(PO.User())
                .yaOpenPage('/ege/?exam_id=4')
                .yaCheckPageUrl('/tutor/ege/?exam_id=4', defaultCheckPageConf, ['exam_id'])
                .yaShouldBeVisible(PO.User());
        });

        it('Пользователь не залогинен', function() {
            return this.browser
                .yaOpenPage('/ege/')
                .yaCheckPageUrl('/tutor/ege/', defaultCheckPageConf)
                .yaShouldBeVisible(PO.UserEnter())
                .yaOpenPage('/ege/?exam_id=4')
                .yaCheckPageUrl('/tutor/ege/?exam_id=4', defaultCheckPageConf, ['exam_id'])
                .yaShouldBeVisible(PO.UserEnter());
        });
    });
});
