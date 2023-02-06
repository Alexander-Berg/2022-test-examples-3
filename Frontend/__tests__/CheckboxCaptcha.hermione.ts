// @ts-nocheck
const elements = require('./page-objects');

// eslint-disable-next-line mocha/no-skipped-tests
describe.skip('CheckboxCaptcha', () => {
  // TODO: Refactor as AdvancedCaptcha.hermione.
  it('should render all captcha statuses', async () => {
    const browser = this.browser;
    await browser
      .openScenario('CheckboxCaptcha', 'DefaultCase')
      .assertView('plain', [elements.container]);
    await browser
      .openScenario('CheckboxCaptcha', 'DefaultCase', { status: 'pending' })
      .assertView('pending', [elements.container]);
    await browser
      .openScenario('CheckboxCaptcha', 'DefaultCase', { status: 'success' })
      .assertView('success', [elements.container]);
    await browser
      .openScenario('CheckboxCaptcha', 'DefaultCase', { status: 'invalid' })
      .assertView('invalid', [elements.container]);
    return browser;
  });
});
