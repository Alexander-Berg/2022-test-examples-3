const { captchaUrl, ignoreElements, ...elements } = require('./page-object');
const { mockOk, mockFailed } = require('./shared');

// interface Window {
//   onChallengeVisibleCalled: boolean;
//   onChallengeHiddenCalled: boolean;

//   smartCaptcha: {
//     subscribe: (id: number, event: string, callback: Function) => void;
//   };
// }

describe('Subscriptions', () => {
  it('should notify user on challenge visible and hidden', function visibilitySubscriptions() {
    return this.browser
      .url(captchaUrl)
      .waitForVisible(elements.checkboxIframe, 10000)
      .execute(mockFailed)
      .execute(function initHandlers() {
        window.onChallengeVisibleCalled = false;
        window.onChallengeHiddenCalled = false;

        function onChallengeVisible() {
          window.onChallengeVisibleCalled = true;
        }

        function onChallengeHidden() {
          window.onChallengeHiddenCalled = true;
        }

        window.smartCaptcha.subscribe(0, 'challenge-visible', onChallengeVisible);
        window.smartCaptcha.subscribe(0, 'challenge-hidden', onChallengeHidden);
      })
      .click(elements.checkboxIframe)
      .waitForVisible(elements.advancedIframe, 10000)
      .execute(function assertVisible() {
        if (!window.onChallengeVisibleCalled) {
          throw new Error('onChallengeVisible was not called');
        }
      })
      .click('body')
      .execute(function assertHidden() {
        if (!window.onChallengeHiddenCalled) {
          throw new Error('onChallengeHidden was not called');
        }
      });
  });

  // eslint-disable-next-line mocha/no-skipped-tests
  it.skip('should notify user on success', function visibilitySubscriptions() {
    return this.browser
      .url(captchaUrl)
      .waitForVisible(elements.checkboxIframe, 10000)
      .execute(mockOk)
      .execute(function initHandlers() {
        window.onSuccessCalled = false;

        function onSuccess() {
          window.onSuccessCalled = true;
        }

        window.smartCaptcha.subscribe(0, 'success', onSuccess);
      })
      .click(elements.checkboxIframe)
      .pause(1000)
      .execute(function assertSuccess() {
        if (!window.onSuccessCalled) {
          throw new Error('onSuccess was not called');
        }
      });
  });
});
