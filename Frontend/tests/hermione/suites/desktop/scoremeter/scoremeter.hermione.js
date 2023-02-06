describe('Балломер', function() {
    const animationDelay = 1500;

    it('Клик в предмет', function() {
        return this.browser
            .yaOpenPage('/ege/?passport_uid=ballomerCase&server_time=1567170623')
            .moveToObject(PO.Scoremeter.Subject())
            .pause(animationDelay)
            .assertView('with-focus', PO.Scoremeter())
            .moveToObject(PO.Scoremeter.Head())
            .pause(animationDelay)
            .assertView('without-focus', PO.Scoremeter());
    });
});
