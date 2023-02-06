// @ts-nocheck
describe('Login', () => {
  describe('theme serp', () => {
    it('should render login', function() {
      return this.browser
        .openScenario('Login', 'Serp')
        .assertView('base', ['.Login_serp_1'])
        .moveToObject('.Login_serp_1')
        .assertView('hovered', ['.Login_serp_1'])
        .execute(function setFocus() {
          window.document.getElementsByClassName('Login_serp_1')[0].focus();
        })
        .assertView('focused', ['.Login_serp_1']);
    });

    it('should render login with picture', function() {
      return this.browser
        .openScenario('Login', 'Serp')
        .assertView('base', ['.Login_serp_2'])
        .moveToObject('.Login_serp_2')
        .assertView('hovered', ['.Login_serp_2'])
        .execute(function setFocus() {
          window.document.getElementsByClassName('Login_serp_2')[0].focus();
        })
        .assertView('focused', ['.Login_serp_2']);
    });
  });
});
