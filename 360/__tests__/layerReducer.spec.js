import {Map} from 'immutable';

import LayerRecord from '../LayerRecord';
import layerReducer, {initialState} from '../layerReducer';
import {ActionTypes} from '../layersConstants';

describe('layerReducer', () => {
  test('должен обрабатывать начальное состояние', () => {
    expect(layerReducer(undefined, {})).toEqual(initialState);
  });

  describe('GET_LAYER', () => {
    test('должен перейти в состояние запроса', () => {
      const state = Map({
        request: false,
        error: true,
        deleting: false,
        data: null
      });
      const expectedState = Map({
        request: true,
        error: false,
        deleting: false,
        data: null
      });
      const action = {
        type: ActionTypes.GET_LAYER
      };
      expect(layerReducer(state, action)).toEqual(expectedState);
    });
  });

  describe('GET_LAYER_SUCCESS', () => {
    test('должен перейти в состояние успешного запроса', () => {
      const state = Map({
        request: true,
        error: false,
        deleting: false,
        data: null
      });
      const expectedState = Map({
        request: false,
        error: false,
        deleting: false,
        data: new LayerRecord({id: 1, name: 'layer'})
      });
      const action = {
        type: ActionTypes.GET_LAYER_SUCCESS,
        layer: {
          id: 1,
          name: 'layer'
        }
      };
      expect(layerReducer(state, action)).toEqual(expectedState);
    });
  });

  describe('GET_LAYER_FAILURE', () => {
    test('должен перейти в состояние ошибки', () => {
      const state = Map({
        request: true,
        error: false,
        deleting: false,
        data: null
      });
      const expectedState = Map({
        request: false,
        error: true,
        deleting: false,
        data: null
      });
      const action = {
        type: ActionTypes.GET_LAYER_FAILURE,
        error: true
      };
      expect(layerReducer(state, action)).toEqual(expectedState);
    });
  });

  describe('UPDATE_LAYER_SUCCESS', () => {
    test('должен обновить данные слоя', () => {
      const state = Map({
        request: false,
        error: false,
        deleting: false,
        data: new LayerRecord({id: 1, name: 'layer'})
      });
      const expectedState = Map({
        request: false,
        error: false,
        deleting: false,
        data: new LayerRecord({id: 1, name: 'layer_1'})
      });
      const action = {
        type: ActionTypes.UPDATE_LAYER_SUCCESS,
        values: {
          name: 'layer_1'
        }
      };
      expect(layerReducer(state, action)).toEqual(expectedState);
    });
  });

  describe('DELETE_LAYER', () => {
    test('должен перейти в состояние удаления', () => {
      const state = Map({
        request: false,
        error: false,
        deleting: false,
        data: new LayerRecord({id: 1})
      });
      const expectedState = Map({
        request: false,
        error: false,
        deleting: true,
        data: new LayerRecord({id: 1})
      });
      const action = {
        type: ActionTypes.DELETE_LAYER,
        id: 1
      };
      expect(layerReducer(state, action)).toEqual(expectedState);
    });
  });

  describe('DELETE_LAYER_SUCCESS', () => {
    test('должен выйти из состояния удаления', () => {
      const state = Map({
        request: false,
        error: false,
        deleting: true,
        data: new LayerRecord({id: 1})
      });
      const expectedState = Map({
        request: false,
        error: false,
        deleting: false,
        data: new LayerRecord({id: 1})
      });
      const action = {
        type: ActionTypes.DELETE_LAYER_SUCCESS,
        id: 1
      };
      expect(layerReducer(state, action)).toEqual(expectedState);
    });
  });

  describe('DELETE_LAYER_FAILURE', () => {
    test('должен выйти из состояния удаления', () => {
      const state = Map({
        request: false,
        error: false,
        deleting: true,
        data: new LayerRecord({id: 1})
      });
      const expectedState = Map({
        request: false,
        error: false,
        deleting: false,
        data: new LayerRecord({id: 1})
      });
      const action = {
        type: ActionTypes.DELETE_LAYER_FAILURE,
        id: 1
      };
      expect(layerReducer(state, action)).toEqual(expectedState);
    });
  });

  describe('RESET_LAYER', () => {
    test('должен сбрасывать данные на начальные', () => {
      const state = Map({
        request: false,
        error: false,
        deleting: false,
        data: new LayerRecord({id: 1})
      });
      const action = {
        type: ActionTypes.RESET_LAYER
      };
      expect(layerReducer(state, action)).toEqual(initialState);
    });
  });
});
