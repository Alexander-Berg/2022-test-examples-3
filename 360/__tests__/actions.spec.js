import checkPropTypes from '../checkPropTypes';
import {makeActionCreator, makeActionWithMetaCreator, getActionType} from '../actions';

jest.mock('../checkPropTypes');

describe('utils/actions', () => {
  describe('getActionType', () => {
    test('должен возвращать строку с типом экшна', () => {
      const featureName = 'FeatureName';
      const actionName = 'ACTION_NAME';
      const expectedActionType = 'FeatureName/ACTION_NAME';

      expect(getActionType(featureName, actionName)).toBe(expectedActionType);
    });
  });

  const FEATURE_NAME = 'SomeBrilliantFeature';
  const ACTION_NAME = 'ACTION_NAME';

  describe('makeActionCreator', () => {
    const actionCreatorFabric = makeActionCreator(FEATURE_NAME);

    beforeEach(() => {
      checkPropTypes.mockReset().mockImplementation(() => {});
    });

    describe('makeActionCreator', () => {
      test('должен записывать в поле type тип экшна', () => {
        const expectedActionType = getActionType(FEATURE_NAME, ACTION_NAME);
        const actionCreator = actionCreatorFabric(ACTION_NAME);

        expect(actionCreator.type).toBe(expectedActionType);
      });

      test('должен бросать ошибку, если передали невалидную метаинформацию', () => {
        expect(() => actionCreatorFabric('pew', {}, {})).toThrow();
        expect(() => actionCreatorFabric('pew', {}, null, {})).toThrow();
      });
    });

    describe('actionCreator', () => {
      test('должен возвращать объект с полями type и payload', () => {
        const actionCreator = actionCreatorFabric(ACTION_NAME);
        const expectedActionType = getActionType(FEATURE_NAME, ACTION_NAME);
        const payload = Symbol();
        const expectedAction = {
          type: expectedActionType,
          payload
        };

        expect(actionCreator(payload)).toEqual(expectedAction);
      });

      test('должен возвращать объект с пустым payload, если его не передали', () => {
        const actionCreator = actionCreatorFabric(ACTION_NAME);
        const expectedActionType = getActionType(FEATURE_NAME, ACTION_NAME);
        const payload = {};
        const expectedAction = {
          type: expectedActionType,
          payload
        };

        expect(actionCreator()).toEqual(expectedAction);
      });

      test('должен возвращать объект с полями type и payload и meta, если был вызван с метой', () => {
        const meta = Symbol();
        const actionCreator = actionCreatorFabric(ACTION_NAME, {}, {}, meta);
        const payload = Symbol();
        const expectedActionType = getActionType(FEATURE_NAME, ACTION_NAME);
        const expectedAction = {
          type: expectedActionType,
          payload,
          meta
        };

        expect(actionCreator(payload)).toEqual(expectedAction);
      });

      test('должен вызывать проверку проптайпов', () => {
        const actionType = getActionType(FEATURE_NAME, ACTION_NAME);
        const payload = Symbol();
        const actionPropTypes = Symbol();
        const actionCreator = actionCreatorFabric(ACTION_NAME, actionPropTypes);

        actionCreator(payload);

        expect(checkPropTypes).toHaveBeenCalledTimes(1);
        expect(checkPropTypes).toHaveBeenCalledWith(actionType, actionPropTypes, payload);
      });
    });
  });

  describe('makeActionWithMetaCreator', () => {
    const actionCreatorFabric = makeActionWithMetaCreator(FEATURE_NAME);

    beforeEach(() => {
      checkPropTypes.mockReset().mockImplementation(() => {});
    });

    test('должен проверять проптайпы для меты', () => {
      const actionType = getActionType(FEATURE_NAME, ACTION_NAME);
      const propTypes = Symbol();
      const metaPropTypes = Symbol();
      const meta = Symbol();
      const actionCreator = actionCreatorFabric(ACTION_NAME, propTypes, metaPropTypes);

      actionCreator(meta);

      expect(checkPropTypes).toHaveBeenCalledTimes(1);
      expect(checkPropTypes.mock.calls[0]).toEqual([actionType, metaPropTypes, meta]);
    });
  });
});
