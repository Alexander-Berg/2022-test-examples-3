import {propsChangeWatcher} from '../debug';

describe('utils/debug', () => {
  test('должен возвращать новые пропы', () => {
    const state = {};
    const props = {someName: 1};
    const expectedStateChanges = {prev_someName: 1};
    const logFunction = jest.fn();

    sinon.stub(global.console, 'log').value(logFunction);

    expect(propsChangeWatcher(props, state)).toEqual(expectedStateChanges);
  });

  test('должен возвращать изменившиеся пропы', () => {
    const state = {prev_someName: 1};
    const props = {someName: 2};
    const expectedStateChanges = {prev_someName: 2};
    const logFunction = jest.fn();

    sinon.stub(global.console, 'log').value(logFunction);

    expect(propsChangeWatcher(props, state)).toEqual(expectedStateChanges);
  });

  test('не должен возвращать пропы, оставшиеся без изменений', () => {
    const state = {prev_someName: 1, prev_someOtherName: 1};
    const props = {someName: 2, someOtherName: 1};
    const expectedStateChanges = {prev_someName: 2};
    const logFunction = jest.fn();

    sinon.stub(global.console, 'log').value(logFunction);

    expect(propsChangeWatcher(props, state)).toEqual(expectedStateChanges);
  });

  test('должен возвращать null, если ничего не изменилось в пропах', () => {
    const state = {prev_someName: 1};
    const props = {someName: 1};
    const expectedStateChanges = null;
    const logFunction = jest.fn();

    sinon.stub(global.console, 'log').value(logFunction);

    expect(propsChangeWatcher(props, state)).toEqual(expectedStateChanges);
  });

  test('должен писать в консоль изменения в пропах', () => {
    const state = {prev_someName: 1};
    const props = {someName: 2};
    const logFunction = jest.fn();

    sinon.stub(global.console, 'log').value(logFunction);
    propsChangeWatcher(props, state);

    expect(logFunction).toHaveBeenCalledTimes(1);
    expect(logFunction).toHaveBeenCalledWith(['someName']);
  });

  test('не должен писать в консоль, если изменений не было', () => {
    const state = {prev_someName: 1};
    const props = {someName: 1};
    const logFunction = jest.fn();

    sinon.stub(global.console, 'log').value(logFunction);
    propsChangeWatcher(props, state);

    expect(logFunction).toHaveBeenCalledTimes(0);
  });
});
