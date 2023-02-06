import createActionMetaInfo from 'middlewares/offlineMiddleware/utils/createActionMetaInfo';

import {ActionTypes} from '../layersConstants';
import {
  getLayers,
  getLayersNetwork,
  getLayersSuccess,
  getLayer,
  getLayerNetwork,
  getLayerSuccess,
  getLayerFailure,
  updateLayer,
  updateLayerSuccess,
  createLayer,
  createLayerSuccess,
  deleteLayer,
  deleteLayerSuccess,
  deleteLayerFailure,
  toggleLayer,
  toggleLayerSuccess,
  shareLayer,
  shareLayerSuccess,
  createFeed,
  createFeedSuccess,
  updateFeed,
  importLayer,
  importLayerSuccess,
  resetLayer,
  createToken
} from '../layersActions';

describe('layersActions', () => {
  describe('getLayers', () => {
    test('должен вернуть экшен GET_LAYERS', () => {
      expect(getLayers()).toEqual({
        type: ActionTypes.GET_LAYERS,
        meta: createActionMetaInfo({network: getLayersNetwork()})
      });
    });
  });

  describe('getLayersSuccess', () => {
    test('должен вернуть экшен GET_LAYERS_SUCCESS', () => {
      expect(getLayersSuccess({layers: [], lastUpdateTs: 123})).toEqual({
        type: ActionTypes.GET_LAYERS_SUCCESS,
        layers: [],
        lastUpdateTs: 123
      });
    });
  });

  describe('getLayer', () => {
    test('должен вернуть экшен GET_LAYER', () => {
      expect(getLayer('100')).toEqual({
        type: ActionTypes.GET_LAYER,
        payload: {id: '100'},
        meta: createActionMetaInfo({network: getLayerNetwork({id: '100'})})
      });
    });
  });

  describe('getLayerSuccess', () => {
    test('должен вернуть экшен GET_LAYER_SUCCESS', () => {
      const layer = {id: '100'};

      expect(getLayerSuccess(layer)).toEqual({
        type: ActionTypes.GET_LAYER_SUCCESS,
        layer
      });
    });
  });

  describe('getLayerFailure', () => {
    test('должен вернуть экшен GET_LAYER_FAILURE', () => {
      const error = {message: 'error'};

      expect(getLayerFailure(error)).toEqual({
        type: ActionTypes.GET_LAYER_FAILURE,
        error
      });
    });
  });

  describe('updateLayer', () => {
    test('должен вернуть экшен UPDATE_LAYER', () => {
      const params = {
        id: '100',
        values: {name: 'new_name'},
        applyNotificationsToEvents: true,
        resolveForm() {},
        rejectForm() {}
      };

      expect(updateLayer(params)).toEqual({
        type: ActionTypes.UPDATE_LAYER,
        id: params.id,
        values: params.values,
        applyNotificationsToEvents: params.applyNotificationsToEvents,
        resolveForm: params.resolveForm,
        rejectForm: params.rejectForm
      });
    });
  });

  describe('updateLayerSuccess', () => {
    test('должен вернуть экшен UPDATE_LAYER_SUCCESS', () => {
      const id = '100';
      const values = {name: 'new_name'};

      expect(updateLayerSuccess(id, values)).toEqual({
        type: ActionTypes.UPDATE_LAYER_SUCCESS,
        id,
        values
      });
    });
  });

  describe('createLayer', () => {
    test('должен вернуть экшен CREATE_LAYER', () => {
      const params = {
        values: {name: 'name'},
        resolveForm() {},
        rejectForm() {}
      };

      expect(createLayer(params)).toEqual({
        type: ActionTypes.CREATE_LAYER,
        values: params.values,
        resolveForm: params.resolveForm,
        rejectForm: params.rejectForm
      });
    });
  });

  describe('createLayerSuccess', () => {
    test('должен вернуть экшен CREATE_LAYER_SUCCESS', () => {
      const values = {name: 'name'};

      expect(createLayerSuccess(values)).toEqual({
        type: ActionTypes.CREATE_LAYER_SUCCESS,
        values
      });
    });
  });

  describe('deleteLayer', () => {
    test('должен вернуть экшен DELETE_LAYER', () => {
      const params = {
        id: '100',
        recipientLayerId: '101',
        resolveForm() {},
        rejectForm() {}
      };

      expect(deleteLayer(params)).toEqual({
        type: ActionTypes.DELETE_LAYER,
        id: params.id,
        recipientLayerId: params.recipientLayerId,
        resolveForm: params.resolveForm,
        rejectForm: params.rejectForm
      });
    });
  });

  describe('deleteLayerSuccess', () => {
    test('должен вернуть экшен DELETE_LAYER_SUCCESS', () => {
      const id = '100';
      const recipientLayerId = '101';

      expect(deleteLayerSuccess(id, recipientLayerId)).toEqual({
        type: ActionTypes.DELETE_LAYER_SUCCESS,
        id,
        recipientLayerId
      });
    });
  });

  describe('deleteLayerFailure', () => {
    test('должен вернуть экшен DELETE_LAYER_FAILURE', () => {
      const id = '100';
      const recipientLayerId = '101';

      expect(deleteLayerFailure(id, recipientLayerId)).toEqual({
        type: ActionTypes.DELETE_LAYER_FAILURE,
        id,
        recipientLayerId
      });
    });
  });

  describe('toggleLayer', () => {
    test('должен вернуть экшен TOGGLE_LAYER', () => {
      const id = '100';
      const checked = false;

      expect(toggleLayer(id, checked)).toEqual({
        type: ActionTypes.TOGGLE_LAYER,
        id,
        checked
      });
    });
  });

  describe('toggleLayerSuccess', () => {
    test('должен вернуть экшен TOGGLE_LAYER_SUCCESS', () => {
      const id = '100';
      const checked = false;

      expect(toggleLayerSuccess(id, checked)).toEqual({
        type: ActionTypes.TOGGLE_LAYER_SUCCESS,
        id,
        checked
      });
    });
  });

  describe('shareLayer', () => {
    test('должен вернуть экшен SHARE_LAYER', () => {
      const params = {
        id: 100,
        name: 'name',
        privateToken: '123'
      };

      expect(shareLayer(params)).toEqual({
        type: ActionTypes.SHARE_LAYER,
        id: params.id,
        name: params.name,
        privateToken: params.privateToken
      });
    });
  });

  describe('shareLayerSuccess', () => {
    test('должен вернуть экшен SHARE_LAYER_SUCCESS', () => {
      const layer = {
        id: 100
      };

      expect(shareLayerSuccess(layer)).toEqual({
        type: ActionTypes.SHARE_LAYER_SUCCESS,
        layer
      });
    });
  });

  describe('createFeed', () => {
    test('должен вернуть экшен CREATE_FEED', () => {
      const params = {
        url: 'url',
        name: 'name'
      };

      expect(createFeed(params)).toEqual({
        type: ActionTypes.CREATE_FEED,
        url: params.url,
        name: params.name
      });
    });
  });

  describe('createFeedSuccess', () => {
    test('должен вернуть экшен CREATE_FEED_SUCCESS', () => {
      const layer = {
        id: 100
      };

      expect(createFeedSuccess(layer)).toEqual({
        type: ActionTypes.CREATE_FEED_SUCCESS,
        layer
      });
    });
  });

  describe('updateFeed', () => {
    test('должен вернуть экшен UPDATE_FEED', () => {
      const id = '100';

      expect(updateFeed(id)).toEqual({
        type: ActionTypes.UPDATE_FEED,
        id
      });
    });
  });

  describe('importLayer', () => {
    test('должен вернуть экшен IMPORT_LAYER', () => {
      const params = {
        values: {},
        resolveForm() {},
        rejectForm() {}
      };

      expect(importLayer(params)).toEqual({
        type: ActionTypes.IMPORT_LAYER,
        values: params.values,
        resolveForm: params.resolveForm,
        rejectForm: params.rejectForm
      });
    });
  });

  describe('importLayerSuccess', () => {
    test('должен вернуть экшен IMPORT_LAYER_SUCCESS', () => {
      const layer = {
        id: 100
      };

      expect(importLayerSuccess(layer)).toEqual({
        type: ActionTypes.IMPORT_LAYER_SUCCESS,
        layer
      });
    });
  });

  describe('resetLayer', () => {
    test('должен вернуть экшен RESET_LAYER', () => {
      expect(resetLayer()).toEqual({
        type: ActionTypes.RESET_LAYER
      });
    });
  });

  describe('createToken', () => {
    test('должен вернуть экшен createToken', () => {
      const params = {
        id: 100,
        forceNew: true
      };
      expect(createToken(params)).toEqual({
        type: ActionTypes.CREATE_TOKEN,
        id: params.id,
        forceNew: params.forceNew
      });
    });
  });
});
