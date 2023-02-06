import { DEFAULT_LINKS } from '@yandex-int/captcha/lib/links-provider';
import { getUrls } from '../get-urls';

describe('getUrls', () => {
  test('should return window origin + endpoint when in production env', () => {
    process.env.NODE_ENV = 'production';

    expect(getUrls()).toEqual({ validationUrl: 'http://localhost/check', greedUrl: 'http://localhost/captchapgrd' });
  });

  test('should return production links in testing and development', () => {
    process.env.NODE_ENV = 'development';
    expect(getUrls()).toEqual({ validationUrl: DEFAULT_LINKS.captchaEndpoint(), greedUrl: DEFAULT_LINKS.greedjs() });

    process.env.NODE_ENV = 'testing';
    expect(getUrls()).toEqual({ validationUrl: DEFAULT_LINKS.captchaEndpoint(), greedUrl: DEFAULT_LINKS.greedjs() });
  });
});
