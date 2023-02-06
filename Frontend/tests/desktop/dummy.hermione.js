describe('dummy', function() {
    it('is true', function() {
        return this.browser
            .loginToGoals()
            .preparePage('dummy', '/')
            .waitForVisible('.GoalsNode')
            .assertView('plain', 'body');
    });
});
