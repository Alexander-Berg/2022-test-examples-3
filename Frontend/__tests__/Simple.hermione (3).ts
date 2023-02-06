// @ts-nocheck
describe('Yaplus', () => {
    it('should render yaplus', function() {
        return this.browser
          .openScenario('Yaplus', 'Simple')
          .assertView('simple', ['.Simple .Yaplus'])
          .assertView('text', ['.Text .Yaplus']);
      });
});
