import sinon from 'sinon';

beforeEach(() => {
  global.sinon = sinon.createSandbox();
});

afterEach(() => {
  if (global.sinon) {
    global.sinon.restore();
  }
});
