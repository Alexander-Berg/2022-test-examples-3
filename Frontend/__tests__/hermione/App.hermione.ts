const { url } = require('./page-object');

describe('External captcha themer', () => {
  it('should load themer', function loadThemer() {
    return this.browser
      .url(url)
      .assertView('base-themer', ['body']);
  });

  it('should change status', async function changeStatus() {
    await this.browser.url(url).assertView('default', ['body']);

    // TODO: Спиннер крутиться, из-за чего тест плавает

    // await this.browser
    //   .execute(() =>
    //     window.postMessage({ type: 'status', payload: 'loading' }, '*'));
    // await this.browser.assertView('loading', ['body'], {
    //   antialiasingTolerance: 10,
    // });

    await this.browser
      .execute(() =>
        postMessage({ type: 'status', payload: 'success' }, '*'));
    await this.browser.assertView('success', ['body']);

    await this.browser
      .execute(() =>
        postMessage({ type: 'status', payload: 'error' }, '*'));
    await this.browser.assertView('error', ['body']);
  });
});
