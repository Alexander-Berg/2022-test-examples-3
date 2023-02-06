import {Map, OrderedMap} from 'immutable';

import {WITHOUT_DECISION_EVENT_COLOR} from 'features/events/eventsConstants';

import {DEFAULT_LAYER_COLOR} from '../layersConstants';
import {
  getLayers,
  getLayerById,
  getToggledOnLayersIds,
  getDefaultLayer,
  getWritableLayers,
  getLayersByTypeFactory,
  getLayersForRelocation,
  getLayersColors,
  makeGetLayerColor
} from '../layersSelectors';
import LayerRecord from '../LayerRecord';

describe('layersSelectors', () => {
  describe('getLayers', () => {
    test('должен вернуть все слои', () => {
      const state = {
        layers: Map({
          byId: Map({
            '100': new LayerRecord({name: 'test1'}),
            '101': new LayerRecord({name: 'test2'})
          })
        })
      };

      expect(getLayers(state)).toEqual(state.layers.get('byId'));
    });
  });

  describe('getLayerById', () => {
    test('должен вернуть слой с заданным id, если он есть', () => {
      const state = {
        layers: Map({
          byId: Map({
            '100': new LayerRecord({name: 'test1'}),
            '101': new LayerRecord({name: 'test2'})
          })
        })
      };

      expect(getLayerById(state, '100')).toEqual(new LayerRecord({name: 'test1'}));
    });

    test('должен вернуть undefined, если нет слоя с заданным id', () => {
      const state = {
        layers: Map({
          byId: Map({
            '100': new LayerRecord({name: 'test1'}),
            '101': new LayerRecord({name: 'test2'})
          })
        })
      };

      expect(getLayerById(state, '102')).toBe(undefined);
    });
  });

  describe('getToggledOnLayersIds', () => {
    test('должен вернуть список id включенных слоев', () => {
      const state = {
        layers: Map({
          byId: Map({
            '100': new LayerRecord({id: '100', isToggledOn: true}),
            '101': new LayerRecord({id: '101', isToggledOn: false}),
            '102': new LayerRecord({id: '102', isToggledOn: true})
          })
        })
      };

      expect(getToggledOnLayersIds(state)).toEqual(['100', '102']);
    });
  });

  describe('getDefaultLayer', () => {
    test('должен вернуть дефолтный слой, если он есть', () => {
      const state = {
        layers: Map({
          byId: Map({
            '123': new LayerRecord({isDefault: true}),
            '321': new LayerRecord({})
          })
        })
      };

      expect(getDefaultLayer(state)).toEqual(new LayerRecord({isDefault: true}));
    });

    test('должен вернуть undefined, если нет дефолтного слоя', () => {
      const state = {
        layers: Map({
          byId: Map({
            '123': new LayerRecord({}),
            '321': new LayerRecord({})
          })
        })
      };

      expect(getDefaultLayer(state)).toBe(undefined);
    });
  });

  describe('getLayersByTypeFactory', () => {
    test('должен вернуть слои с переданным типом', () => {
      const state = {
        layers: Map({
          byId: Map({
            '111': new LayerRecord({type: 'user'}),
            '112': new LayerRecord({type: 'feed'}),
            '113': new LayerRecord({type: 'user'})
          })
        })
      };

      expect(getLayersByTypeFactory('feed')(state)).toEqual(
        OrderedMap({
          '112': new LayerRecord({type: 'feed'})
        })
      );
    });

    test('должен вернуть слои с переданными типами', () => {
      const state = {
        layers: Map({
          byId: Map({
            '111': new LayerRecord({type: 'user'}),
            '112': new LayerRecord({type: 'feed'}),
            '113': new LayerRecord({type: 'user'}),
            '114': new LayerRecord({type: 'absence'})
          })
        })
      };

      expect(getLayersByTypeFactory(['user', 'feed'])(state)).toEqual(
        OrderedMap({
          '111': new LayerRecord({type: 'user'}),
          '112': new LayerRecord({type: 'feed'}),
          '113': new LayerRecord({type: 'user'})
        })
      );
    });
  });

  describe('getWritableLayers', () => {
    test('должен вернуть календари, в которых можно создавать события', () => {
      const state = {
        layers: Map({
          byId: Map({
            '111': new LayerRecord({canAddEvent: false}),
            '112': new LayerRecord({canAddEvent: true}),
            '113': new LayerRecord({canAddEvent: false})
          })
        })
      };

      expect(getWritableLayers(state)).toEqual(
        OrderedMap({
          '112': new LayerRecord({canAddEvent: true})
        })
      );
    });
  });

  describe('getLayersForRelocation', () => {
    test('должен вернуть списки в которые можно переместить события', () => {
      const state = {
        layers: Map({
          byId: Map({
            '111': new LayerRecord({id: '111', canAddEvent: false}),
            '112': new LayerRecord({id: '112', canAddEvent: true}),
            '113': new LayerRecord({id: '113', canAddEvent: true})
          })
        })
      };

      expect(getLayersForRelocation('112')(state)).toEqual(
        OrderedMap({
          '113': new LayerRecord({id: '113', canAddEvent: true})
        })
      );
    });
  });

  describe('makeGetLayerColor', () => {
    test('должен вернуть WITHOUT_DECISION_EVENT_COLOR при event.isWithoutDecision', () => {
      const event = {
        isWithoutDecision: true
      };

      const selector = makeGetLayerColor();

      expect(selector.resultFunc(null, event)).toBe(WITHOUT_DECISION_EVENT_COLOR);
    });

    test('должен вернуть цвет слоя, id которого указан в event', () => {
      const layers = new Map({
        1234: {color: '#000000'}
      });

      const event = new Map({
        layerId: 1234
      });

      const selector = makeGetLayerColor();

      expect(selector.resultFunc(layers, event)).toBe('#000000');
    });
    test('должен вернуть DEFAULT_LAYER_COLOR, если нет слоя, указанного в event.get("layerId")', () => {
      const layers = new Map({
        1234: {color: '#000000'}
      });

      const event = new Map({
        layerId: 123
      });

      const selector = makeGetLayerColor();

      expect(selector.resultFunc(layers, event)).toBe(DEFAULT_LAYER_COLOR);
    });
    test('должен вернуть DEFAULT_LAYER_COLOR, если нет layerId', () => {
      const layers = new Map({
        1234: {color: '#000000'}
      });

      const event = new Map({});

      const selector = makeGetLayerColor();

      expect(selector.resultFunc(layers, event)).toBe(DEFAULT_LAYER_COLOR);
    });
    test('должен вернуть transparent, если нет layerId и в фабрику передан transparent', () => {
      const layers = new Map({
        1234: {color: '#000000'}
      });

      const event = new Map({});

      const selector = makeGetLayerColor('transparent');

      expect(selector.resultFunc(layers, event)).toBe('transparent');
    });
  });

  describe('getLayersColors', () => {
    test('должен возвращать цвета слоёв', () => {
      const color1 = 'color1';
      const color2 = 'color2';
      const color3 = 'color3';
      const state = {
        layers: Map({
          byId: Map({
            '111': new LayerRecord({id: '111', color: color1}),
            '112': new LayerRecord({id: '112', color: color2}),
            '113': new LayerRecord({id: '113', color: color3})
          })
        })
      };

      expect(getLayersColors(state)).toEqual({
        111: color1,
        112: color2,
        113: color3
      });
    });
  });
});
