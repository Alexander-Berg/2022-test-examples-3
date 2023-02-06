import { debounce } from './timeout-utils';

describe('debounce, immediate: false', () => {
  it('should run a function only once within default debounce time', done => {
    let counter = 0;
    const debounced = debounce(() => counter++, 200);

    debounced();
    debounced();
    debounced();
    debounced();
    debounced();

    setTimeout(() => {
      expect(counter).toEqual(1);
      done();
    }, 201);
  });

  it('should run a function when the debounce timeout has passed', done => {
    const stubFn = jest.fn();
    const debounced = debounce(stubFn, 100);

    debounced();
    debounced();
    debounced();
    debounced();
    debounced();

    // NOTE: we set the timeout slightly past debounce time to see if the function is called
    setTimeout(() => {
      debounced();
    }, 105);

    setTimeout(() => {
      expect(stubFn).toBeCalledTimes(2);
      done();
    }, 300);
  });
});

describe('debounce, immediate: true', () => {
  it('should run a function right away if immediate is true', done => {
    let counter = 0;
    const debounced = debounce(() => counter++, 100, true);

    debounced();
    debounced();
    debounced();
    debounced();
    debounced();

    setTimeout(() => {
      expect(counter).toEqual(1);
      done();
    }, 0);
  });

  it('should run a function when the debounce timeout has passed', done => {
    const stubFn = jest.fn();
    const debounced = debounce(stubFn, 100, true);

    debounced();
    debounced();
    debounced();
    debounced();
    debounced();

    setTimeout(() => {
      expect(stubFn).toBeCalledTimes(1);
    }, 0);

    // NOTE: we set the timeout slightly past debounce time to see if the function is called
    setTimeout(() => {
      debounced();
    }, 105);

    setTimeout(() => {
      expect(stubFn).toBeCalledTimes(2);
      done();
    }, 300);
  });
});
