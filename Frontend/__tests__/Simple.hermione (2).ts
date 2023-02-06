// @ts-nocheck
describe('Signup', () => {
  it('should render signup', function() {
    return this.browser
      .openScenario('Signup', 'Simple')
      .assertView('simple', ['.Signup'])
      .moveToObject('.Signup')
      .assertView('hovered', ['.Signup'])
      .execute(function setFocus() {
        window.document.getElementsByClassName('Signup')[0].focus();
      })
      .assertView('focused', ['.Signup']);
  });
});
