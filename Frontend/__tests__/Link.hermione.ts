// @ts-nocheck
describe('Login', () => {
  describe('theme link', () => {
    it('should render login', function() {
      return this.browser
        .openScenario('Login', 'Link')
        .assertView('base', ['.Login_link_1'])
        .moveToObject('.Login_link_1')
        .assertView('hovered', ['.Login_link_1'])
        .execute(function setFocus() {
          window.document.getElementsByClassName('Login_link_1')[0].focus();
        })
        .assertView('focused', ['.Login_link_1']);
    });

    it('should render login with picture', function() {
      return this.browser
        .openScenario('Login', 'Link')
        .assertView('base', ['.Login_link_2'])
        .moveToObject('.Login_link_2')
        .assertView('hovered', ['.Login_link_2'])
        .execute(function setFocus() {
          window.document.getElementsByClassName('Login_link_2')[0].focus();
        })
        .assertView('focused', ['.Login_link_2']);
    });
  });
});
