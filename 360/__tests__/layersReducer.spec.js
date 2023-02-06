import {Map} from 'immutable';

import LayerRecord from '../LayerRecord';
import createLayersReducer from '../layersReducer';
import {ActionTypes} from '../layersConstants';

const layersReducer = createLayersReducer();

describe('layersReducer', () => {
  test('должен обрабатывать начальное состояние', () => {
    const now = Symbol();

    jest.spyOn(Date, 'now').mockReturnValue(now);

    expect(createLayersReducer()(undefined, {})).toEqual(
      Map({
        byId: new Map(),
        lastUpdateTs: now
      })
    );
  });

  describe('TOGGLE_LAYER_SUCCESS', () => {
    test('должен включить слой', () => {
      const state = Map({
        byId: Map({
          1: new LayerRecord({id: 1, name: 'layer_1', isToggledOn: false}),
          2: new LayerRecord({id: 2, name: 'layer_2'})
        })
      });
      const expectedState = Map({
        byId: Map({
          1: new LayerRecord({id: 1, name: 'layer_1', isToggledOn: true}),
          2: new LayerRecord({id: 2, name: 'layer_2'})
        })
      });
      const action = {
        type: ActionTypes.TOGGLE_LAYER_SUCCESS,
        id: 1,
        checked: true
      };
      expect(layersReducer(state, action)).toEqual(expectedState);
    });

    test('должен выключить слой', () => {
      const state = Map({
        byId: Map({
          1: new LayerRecord({id: 1, name: 'layer_1'}),
          2: new LayerRecord({id: 2, name: 'layer_2', isToggledOn: true})
        })
      });
      const expectedState = Map({
        byId: Map({
          1: new LayerRecord({id: 1, name: 'layer_1'}),
          2: new LayerRecord({id: 2, name: 'layer_2', isToggledOn: false})
        })
      });
      const action = {
        type: ActionTypes.TOGGLE_LAYER_SUCCESS,
        id: 2,
        checked: false
      };
      expect(layersReducer(state, action)).toEqual(expectedState);
    });
  });

  describe('GET_LAYERS_SUCCESS', () => {
    test('должен заменить слои в списке', () => {
      const state = Map({
        byId: Map({
          1: new LayerRecord({id: 1, name: 'layer_1'})
        })
      });
      const expectedState = Map({
        byId: Map({
          1: new LayerRecord({id: 1, name: 'layer_1'}),
          2: new LayerRecord({id: 2, name: 'layer_2'})
        }),
        lastUpdateTs: undefined
      });
      const action = {
        type: ActionTypes.GET_LAYERS_SUCCESS,
        layers: [{id: 1, name: 'layer_1'}, {id: 2, name: 'layer_2'}]
      };
      expect(layersReducer(state, action)).toEqual(expectedState);
    });
  });

  describe('UPDATE_LAYER_SUCCESS', () => {
    test('должен поменять дефолтный слой', () => {
      const state = Map({
        byId: Map({
          1: new LayerRecord({id: 1, name: 'layer_1', isDefault: false}),
          2: new LayerRecord({id: 2, name: 'layer_2', isDefault: true})
        })
      });
      const expectedState = Map({
        byId: Map({
          1: new LayerRecord({id: 1, name: 'layer_1', isDefault: true}),
          2: new LayerRecord({id: 2, name: 'layer_2', isDefault: false})
        })
      });
      const action = {
        type: ActionTypes.UPDATE_LAYER_SUCCESS,
        id: 1,
        values: {
          isDefault: true
        }
      };
      expect(layersReducer(state, action)).toEqual(expectedState);
    });

    test('должен обновить слой', () => {
      const state = Map({
        byId: Map({
          1: new LayerRecord({id: 1, name: 'layer_1'}),
          2: new LayerRecord({id: 2, name: 'layer_2'})
        })
      });
      const expectedState = Map({
        byId: Map({
          1: new LayerRecord({id: 1, name: 'layer_1'}),
          2: new LayerRecord({id: 2, name: 'changed_name'})
        })
      });
      const action = {
        type: ActionTypes.UPDATE_LAYER_SUCCESS,
        id: 2,
        values: {
          name: 'changed_name'
        }
      };
      expect(layersReducer(state, action)).toEqual(expectedState);
    });
  });

  describe('CREATE_LAYER_SUCCESS', () => {
    test('должен добавить новый слой и сделать его дефолтным', () => {
      const state = Map({
        byId: Map({
          1: new LayerRecord({id: 1, name: 'layer_1', isDefault: false}),
          2: new LayerRecord({id: 2, name: 'layer_2', isDefault: true})
        })
      });
      const expectedState = Map({
        byId: Map({
          1: new LayerRecord({id: 1, name: 'layer_1', isDefault: false}),
          2: new LayerRecord({id: 2, name: 'layer_2', isDefault: false}),
          3: new LayerRecord({id: 3, name: 'layer_3', isDefault: true})
        })
      });
      const action = {
        type: ActionTypes.CREATE_LAYER_SUCCESS,
        values: {
          id: 3,
          name: 'layer_3',
          isDefault: true
        }
      };

      expect(layersReducer(state, action)).toEqual(expectedState);
    });

    test('должен добавить новый слой', () => {
      const state = Map({
        byId: Map({})
      });
      const expectedState = Map({
        byId: Map({
          1: new LayerRecord({id: 1, name: 'layer_1', isDefault: true})
        })
      });
      const action = {
        type: ActionTypes.CREATE_LAYER_SUCCESS,
        values: {
          id: 1,
          name: 'layer_1',
          isDefault: true
        }
      };

      expect(layersReducer(state, action)).toEqual(expectedState);
    });
  });

  describe('DELETE_LAYER_SUCCESS', () => {
    test('должен удалить дефолтный слой и назначить новый дефолтный слой, если возможно', () => {
      const state = Map({
        byId: Map({
          1: new LayerRecord({
            id: 1,
            name: 'layer_1',
            type: 'user',
            isDefault: true,
            isOwner: true
          }),
          2: new LayerRecord({
            id: 2,
            name: 'layer_2',
            type: 'user',
            isDefault: false,
            isOwner: false
          }),
          3: new LayerRecord({
            id: 3,
            name: 'layer_3',
            type: 'feed',
            isDefault: false,
            isOwner: true
          }),
          4: new LayerRecord({
            id: 4,
            name: 'layer_4',
            type: 'user',
            isDefault: false,
            isOwner: true
          })
        })
      });
      const expectedState = Map({
        byId: Map({
          2: new LayerRecord({
            id: 2,
            name: 'layer_2',
            type: 'user',
            isDefault: false,
            isOwner: false
          }),
          3: new LayerRecord({
            id: 3,
            name: 'layer_3',
            type: 'feed',
            isDefault: false,
            isOwner: true
          }),
          4: new LayerRecord({id: 4, name: 'layer_4', type: 'user', isDefault: true, isOwner: true})
        })
      });
      const action = {
        type: ActionTypes.DELETE_LAYER_SUCCESS,
        id: 1
      };

      expect(layersReducer(state, action)).toEqual(expectedState);
    });

    test('должен удалить слой', () => {
      const state = Map({
        byId: Map({
          1: new LayerRecord({
            id: 1,
            name: 'layer_1',
            type: 'user',
            isDefault: true,
            isOwner: true
          }),
          2: new LayerRecord({
            id: 2,
            name: 'layer_2',
            type: 'user',
            isDefault: false,
            isOwner: false
          }),
          3: new LayerRecord({
            id: 3,
            name: 'layer_3',
            type: 'feed',
            isDefault: false,
            isOwner: true
          }),
          4: new LayerRecord({
            id: 4,
            name: 'layer_4',
            type: 'user',
            isDefault: false,
            isOwner: true
          })
        })
      });
      const expectedState = Map({
        byId: Map({
          1: new LayerRecord({
            id: 1,
            name: 'layer_1',
            type: 'user',
            isDefault: true,
            isOwner: true
          }),
          2: new LayerRecord({
            id: 2,
            name: 'layer_2',
            type: 'user',
            isDefault: false,
            isOwner: false
          }),
          3: new LayerRecord({
            id: 3,
            name: 'layer_3',
            type: 'feed',
            isDefault: false,
            isOwner: true
          })
        })
      });
      const action = {
        type: ActionTypes.DELETE_LAYER_SUCCESS,
        id: 4
      };

      expect(layersReducer(state, action)).toEqual(expectedState);
    });
  });

  describe('SHARE_LAYER_SUCCESS', () => {
    test('должен добавить слой в список', () => {
      const state = Map({
        byId: Map({
          1: new LayerRecord({id: 1, name: 'layer_1'})
        })
      });
      const expectedState = Map({
        byId: Map({
          1: new LayerRecord({id: 1, name: 'layer_1'}),
          2: new LayerRecord({id: 2, name: 'layer_2'})
        })
      });
      const action = {
        type: ActionTypes.SHARE_LAYER_SUCCESS,
        layer: {id: 2, name: 'layer_2'}
      };
      expect(layersReducer(state, action)).toEqual(expectedState);
    });
  });

  describe('CREATE_FEED_SUCCESS', () => {
    test('должен добавить слой в список', () => {
      const state = Map({
        byId: Map({
          1: new LayerRecord({id: 1, name: 'layer_1'})
        })
      });
      const expectedState = Map({
        byId: Map({
          1: new LayerRecord({id: 1, name: 'layer_1'}),
          2: new LayerRecord({id: 2, name: 'layer_2'})
        })
      });
      const action = {
        type: ActionTypes.CREATE_FEED_SUCCESS,
        layer: {id: 2, name: 'layer_2'}
      };
      expect(layersReducer(state, action)).toEqual(expectedState);
    });
  });

  describe('IMPORT_LAYER_SUCCESS', () => {
    test('должен добавить слой в список', () => {
      const state = Map({
        byId: Map({
          1: new LayerRecord({id: 1, name: 'layer_1'})
        })
      });
      const expectedState = Map({
        byId: Map({
          1: new LayerRecord({id: 1, name: 'layer_1'}),
          2: new LayerRecord({id: 2, name: 'layer_2'})
        })
      });
      const action = {
        type: ActionTypes.IMPORT_LAYER_SUCCESS,
        layer: {id: 2, name: 'layer_2'}
      };
      expect(layersReducer(state, action)).toEqual(expectedState);
    });
  });
});
