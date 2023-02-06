import { render } from '../render';
import { initSmartCaptcha } from './shared';

import '../../typings/global.d';

describe('test flag', () => {
  beforeEach(() => {
    initSmartCaptcha();

    document.body.innerHTML = `
      <div id="container"></div>
    `;
  });

  test('should set test=false default', () => {
    const container = document.getElementById('container') as HTMLElement;

    render(container, {
      sitekey: 'dbg_fJskLdjs3Jsdd3Sd',
    });

    const html = document.body.innerHTML;
    const tests = html.match(/test=false/gi);

    expect(tests.length).toBe(2);
  });

  test('should set test=true when window.smartCaptcha._test=true', () => {
    window.smartCaptcha._test = 'true';

    const container = document.getElementById('container') as HTMLElement;

    render(container, {
      sitekey: 'dbg_fJskLdjs3Jsdd3Sd',
    });

    const html = document.body.innerHTML;
    const tests = html.match(/test=true/gi);

    expect(tests.length).toBe(2);
  });

  test('should set test=false when window.smartCaptcha._test=false', () => {
    window.smartCaptcha._test = 'false';

    const container = document.getElementById('container') as HTMLElement;

    render(container, {
      sitekey: 'dbg_fJskLdjs3Jsdd3Sd',
    });

    const html = document.body.innerHTML;
    const tests = html.match(/test=false/gi);

    expect(tests.length).toBe(2);
  });
});
