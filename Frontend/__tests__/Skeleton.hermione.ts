// @ts-nocheck
describe('Skeleton', () => {
    hermione.skip.in(['win-ie11']);
    it('should render simple example', function() {
        return this.browser
            .openScenario('Skeleton', 'Simple')
            .assertView('skeleton', '.skeleton');
    });
});
