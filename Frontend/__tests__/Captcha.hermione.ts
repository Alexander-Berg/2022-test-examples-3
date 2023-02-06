// @ts-nocheck
const elements = require('./page-objects');

// eslint-disable-next-line mocha/no-skipped-tests
describe.skip('Captcha', () => {
  it('should successful validate checkbox captcha', () => {
    return this.browser
      .openScenario('Captcha', 'CheckboxCase')
      .click(elements.checkboxCaptcha)
      .assertView('valid-checkbox', [elements.container]);
  });

  it('should successful validate advanced captcha', () => {
    return this.browser
      .openScenario('Captcha', 'AdvancedCase')
      .click(elements.checkboxCaptcha)
      .assertView('invalid-checkbox', [elements.container])
      .click(elements.submit)
      .assertView('invalid-form', [elements.container])
      .click(elements.submit)
      .assertView('valid-checkbox', [elements.container]);
  });

  it('should refresh resources after refresh click', () => {
    return this.browser
      .openScenario('Captcha', 'AdvancedCase')
      .click(elements.checkboxCaptcha)
      .assertView('start-resource', [elements.adavncedCaptcha])
      .click(elements.refresh)
      .assertView('next-resource', [elements.adavncedCaptcha]);
  });

  // TODO: Impl this.
  // it('should ignore validate for checkbox while opened form');
  // it('should prevent validate after success');
});
