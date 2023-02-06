function mockFailed() {
  window.fetch = function mockFailed() {
    return new Promise(function mock(resolve) {
      resolve({
        json() {
          return new Promise(function data(resolve) {
            resolve({
              status: 'failed',
              captcha: {},
            });
          });
        },
      });
    });
  };
}

function mockOk() {
  window.fetch = function mockOk() {
    return new Promise(function mock(resolve) {
      resolve({
        json() {
          return new Promise(function data(resolve) {
            resolve({
              status: 'ok',
              spravka: 'mock_spravka',
            });
          });
        },
      });
    });
  };
}

module.exports = {
  mockOk,
  mockFailed,
};
