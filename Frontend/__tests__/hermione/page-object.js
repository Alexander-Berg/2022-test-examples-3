const mainPO = {
  captchaUrl: '/hermione/index.html',
  invisibleCaptchaUrl: '/hermione/invisibleCaptcha.html',
  checkboxUrl: '/hermione/checkbox.html',
  advancedUrl: '/hermione/advanced.html',

  // Containers
  container: '[data-testid="smartCaptcha-container"]',
  advancedContainer: '[data-testid="advanced-container"]',

  // Buttons
  submit: '[data-testid="submit"]',

  // Iframes
  checkboxIframe: 'iframe[data-testid="checkbox-iframe"]',
  advancedIframe: 'iframe[data-testid="advanced-iframe"]',

  ignoreElements: ['.AdvancedCaptcha-Image'],
};

module.exports = mainPO;

// Object.keys({ ...PO, ...mainPO }).reduce(
//   (acc, key) => ({ ...acc, [key]: mainPO[key] || PO[key] }),
//   {},
// );
