import _sinon from 'sinon';

// Mock rum/error-counter
[
  '@yandex-int/rum-counter/dist/bundle/implementation',
  '@yandex-int/rum-counter/dist/bundle/onload',
  '@yandex-int/rum-counter/dist/bundle/send',
  '@yandex-int/error-counter/dist/logError'
].forEach(path => {
  jest.mock(path, () => undefined);
});

jest.mock('@/config', () => ({}));

// Init sinon sandbox
beforeEach(() => {
  const sinon = _sinon.createSandbox();

  // Stub window.Ya
  window.Ya = {
    Rum: {
      sendHeroElement: jest.fn(),
      logError: jest.fn(),
      ERROR_LEVEL: 'error'
    },
    Metrika: jest.fn()
  };

  global.sinon = sinon;
});

afterEach(() => {
  if (global.sinon) {
    global.sinon.restore();
  }
});
