const { captchaUrl, invisibleCaptchaUrl, ...elements } = require('./page-object');
const { mockOk, mockFailed } = require('./shared');

// eslint-disable-next-line mocha/no-skipped-tests
describe('External captcha', () => {
  it('should load captcha', function loadCaptcha() {
    return this.browser
      .url(captchaUrl)
      .waitForVisible(elements.checkboxIframe, 10000)
      .assertView('base-captcha', [elements.checkboxIframe])
      .switchToFrame(elements.checkboxIframe)
      .execute(mockFailed)
      .frameParent()
      .click(elements.checkboxIframe)
      .waitForVisible(elements.advancedIframe, 10000)
      .assertView('open-advanced-captcha', [elements.checkboxIframe, elements.advancedIframe], { antialiasingTolerance: 6 })
      .switchToFrame(elements.advancedIframe)
      .waitForVisible(elements.submit, 10000)

      // Submit
      .click(elements.submit)
      .frameParent()
      .assertView('submit-captcha-error', [elements.checkboxIframe, elements.advancedIframe])
      .frameParent()
      .switchToFrame(elements.checkboxIframe)
      .execute(mockOk)
      .frameParent()
      .switchToFrame(elements.advancedIframe)
      .click(elements.submit)
      .frameParent()
      .assertView('submit-captcha-success', [elements.checkboxIframe, elements.advancedIframe]);
  });

  it('should hide advancedContainer on outside click', function outsideClick() {
    return this.browser
      .url(captchaUrl)
      .waitForVisible(elements.checkboxIframe, 10000)
      .click(elements.checkboxIframe)
      .waitForVisible(elements.advancedIframe, 10000)
      .assertView('should-show-advanced', [elements.checkboxIframe, elements.advancedIframe])

      .click('body')
      .assertView('should-close-advanced', [elements.checkboxIframe]);
  });

  it('should show error when showError called', function showError() {
    return this.browser
      .url(captchaUrl)
      .waitForVisible(elements.checkboxIframe, 10000)
      .execute(() => {
        window.smartCaptcha.showError();
      })
      .assertView('show-error', [elements.checkboxIframe])
      .execute(() => {
        window.smartCaptcha.reset();
      })
      .assertView('reset-show-error', [elements.checkboxIframe]);
  });

  it('should move focus back to checkbox, when we close advanced modal', function() {
    return this.browser
      .url(captchaUrl)
      .waitForVisible(elements.checkboxIframe, 10000)
      .click(elements.checkboxIframe)
      .waitForVisible(elements.advancedIframe, 10000)
      .click('body')
      .execute(() => {
        if (document.activeElement.getAttribute('data-testid') !== 'checkbox-iframe') {
          throw new Error('Focus was not moved back to checkbox');
        }
      });
  });

  it('should move focus back, when we close advanced modal in "invisible" mode', function() {
    return this.browser
      .url(invisibleCaptchaUrl)
      .waitForVisible(elements.submit, 10000)
      .click(elements.submit)
      .waitForVisible(elements.advancedIframe, 10000)
      .click('body')
      .execute(() => {
        if (document.activeElement.getAttribute('data-testid') !== 'submit') {
          throw new Error('Focus was not moved back');
        }
      });
  });
});
