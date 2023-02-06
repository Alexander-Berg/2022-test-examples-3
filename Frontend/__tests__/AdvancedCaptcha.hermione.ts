// @ts-nocheck
const elements = require('./page-objects');

// eslint-disable-next-line mocha/no-skipped-tests
describe.skip('AdvancedCaptcha', () => {
  it('should render captcha with wide width', () => {
    return this.browser
      .openScenario('AdvancedCaptcha', 'DefaultCase', { width: 'wide' })
      .assertView('plain', [elements.container]);
  });

  it('should render invalid image captcha', () => {
    return this.browser
      .openScenario('AdvancedCaptcha', 'DefaultCase', { status: 'invalid' })
      .assertView('plain', [elements.container]);
  });

  it('should render invalid voice captcha', () => {
    return this.browser
      .openScenario('AdvancedCaptcha', 'DefaultCase', { status: 'invalid' })
      .click(elements.type)
      .assertView('plain', [elements.container]);
  });

  it('should render image captcha with tooltips', () => {
    return this.browser
      .openScenario('AdvancedCaptcha', 'DefaultCase')
      .moveToObject(elements.tip)
      .assertView('tip', [elements.container])
      .moveToObject(elements.refresh)
      .assertView('refresh', [elements.container])
      .moveToObject(elements.type)
      .assertView('type', [elements.container])
      .moveToObject(elements.info)
      .assertView('info', [elements.container]);
  });

  it('should render voice captcha with tooltips', () => {
    return this.browser
      .openScenario('AdvancedCaptcha', 'DefaultCase')
      .click(elements.type)
      .moveToObject(elements.tip)
      .assertView('tip', [elements.container])
      .moveToObject(elements.type)
      .assertView('type', [elements.container]);
  });

  it('should render playing voice captcha', () => {
    return this.browser
      .openScenario('AdvancedCaptcha', 'DefaultCase')
      .click(elements.type)
      .click(elements.play)
      .assertView('plain', [elements.container]);
  });
});
