import PropTypes from 'prop-types';

import checkPropTypes from '../checkPropTypes';

describe('utils/checkPropTypes', () => {
  const checker = jest.fn();

  beforeEach(() => {
    sinon.stub(PropTypes, 'checkPropTypes').value(checker);
  });

  test('не должен ломаться, если не передали propTypes', () => {
    expect(() => checkPropTypes()).not.toThrow();
  });

  test('должен проверять типы пропов по переданным propTypes', () => {
    const propTypes = {
      someProp: PropTypes.bool,
      someOtherProp: PropTypes.func
    };
    const sourceName = 'someFunction';
    const someProp = Symbol();
    const someOtherProp = Symbol();
    const props = {someProp, someOtherProp};

    // чтобы не завязываться на порядок выдачи свойств объекта в тесте
    const keys = Object.keys(propTypes);

    checkPropTypes(sourceName, propTypes, props);

    expect(PropTypes.checkPropTypes).toHaveBeenCalledTimes(2);
    expect(PropTypes.checkPropTypes.mock.calls[0]).toEqual([propTypes, props, keys[0], sourceName]);
    expect(PropTypes.checkPropTypes.mock.calls[1]).toEqual([propTypes, props, keys[1], sourceName]);
  });
});
