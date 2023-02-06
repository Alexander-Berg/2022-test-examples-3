// @ts-nocheck
describe('Login', () => {
  describe('theme action', () => {
    it('should render login', function() {
      return this.browser
        .openScenario('Login', 'Action')
        .assertView('base', ['.Login_action_1'])
        .openScenario('Login', 'Action')
        .moveToObject('.Login_action_1')
        .assertView('hovered', ['.Login_action_1'])
        .execute(function setFocus() {
          window.document.getElementsByClassName('Login_action_1')[0].focus();
        })
        .assertView('focused', ['.Login_action_1']);
    });

    it('should render login with picture', function() {
      return this.browser
        .openScenario('Login', 'Action')
        .assertView('base', ['.Login_action_2'])
        .moveToObject('.Login_action_2')
        .assertView('hovered', ['.Login_action_2'])
        .execute(function setFocus() {
          window.document.getElementsByClassName('Login_action_2')[0].focus();
        })
        .assertView('focused', ['.Login_action_2']);
    });

    it('should render login white', function() {
      return this.browser
        .openScenario('Login', 'Action')
        .assertView('base', ['.Login_action_3'])
        .moveToObject('.Login_action_3')
        .assertView('hovered', ['.Login_action_3'])
        .execute(function setFocus() {
          window.document.getElementsByClassName('Login_action_3')[0].focus();
        })
        .assertView('focused', ['.Login_action_3']);
    });

    it('should render login white with picture', function() {
      return this.browser
        .openScenario('Login', 'Action')
        .assertView('base', ['.Login_action_4'])
        .moveToObject('.Login_action_4')
        .assertView('hovered', ['.Login_action_4'])
        .execute(function setFocus() {
          window.document.getElementsByClassName('Login_action_4')[0].focus();
        })
        .assertView('focused', ['.Login_action_4']);
    });
  });
});
