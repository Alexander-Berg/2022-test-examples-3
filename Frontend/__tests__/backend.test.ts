import fetchMock from 'jest-fetch-mock';
import { Backend } from '../backend';

jest.mock('../../utils/load-script', () => {
  const originalModule = jest.requireActual('../../utils/load-script');

  return {
    ...originalModule,
    loadScript: (_, onSuccess: Function) => {
      onSuccess();
    },
  };
});

function mockValidateFail({ captchaKey }: { captchaKey?: string } = {}) {
  fetchMock.mockOnce(JSON.stringify({
    captcha: {
      key: captchaKey,
    },
    status: 'failed',
  }));
}

describe('backend', () => {
  const backend = new Backend({ sitekey: 'mockSitekey' });

  beforeEach(() => {
    backend.captchaKey = 'initial-key';
    backend.fingerprint = 'fingerprint';
  });

  test('should update `captchaKey` after validate', async () => {
    for (let captchaKey of ['new-key-1', 'new-key-2', 'new-key-3']) {
      mockValidateFail({ captchaKey });
      await backend.validate();

      expect(backend.captchaKey).toBe(captchaKey);
    }
  });

  test('should update `captchaKey` after refresh', async () => {
    for (let captchaKey of ['new-key-1', 'new-key-2', 'new-key-3']) {
      mockValidateFail({ captchaKey });
      await backend.refresh();

      expect(backend.captchaKey).toBe(captchaKey);
    }
  });

  test('should return `hasAnswer` field on validate', async () => {
    mockValidateFail();
    const response = await backend.validate();
    const hasAnswer = response.status === 'resources' && response.hasAnswer;

    expect(hasAnswer).toBeDefined();
    expect(hasAnswer).toBe(false);
  });

  test('should return `hasAnswer` equals false when called validate without answer', async () => {
    mockValidateFail();
    const response = await backend.validate();
    const hasAnswer = response.status === 'resources' && response.hasAnswer;

    expect(hasAnswer).toBe(false);
  });

  test('should return `hasAnswer` equals true when called validate without answer', async () => {
    mockValidateFail();
    const response = await backend.validate('answer');
    const hasAnswer = response.status === 'resources' && response.hasAnswer;

    expect(hasAnswer).toBe(true);
  });

  test('should set `captchaKey` to null on reset', () => {
    backend.reset();

    expect(backend.captchaKey).toBeNull();
  });
});
