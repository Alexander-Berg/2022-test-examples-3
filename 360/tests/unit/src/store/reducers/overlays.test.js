import deepFreeze from 'deep-freeze';
import overlays from '../../../../../src/store/reducers/overlays';
import { OPEN_PANE, CLOSE_PANE, OPEN_SLIDER, CLOSE_SLIDER, SET_ACTION_BARS_VISIBILITY } from '../../../../../src/store/action-types';

describe('overlays reducer', () => {
    let defaultState;
    beforeEach(() => {
        defaultState = { activePane: {} };
        deepFreeze(defaultState);
    });

    it('состояние по умолчанию', () => {
        expect(overlays(undefined, {})).toEqual(defaultState);
    });

    it('открыть панель', () => {
        expect(overlays(defaultState, { type: OPEN_PANE, pane: { type: 'file-info' } }))
            .toEqual({ activePane: { type: 'file-info' } });
    });

    it('закрыть панель', () => {
        expect(overlays(defaultState, { type: CLOSE_PANE }))
            .toEqual({ activePane: {} });
    });

    it('открыть слайдер', () => {
        expect(overlays(defaultState, { type: OPEN_SLIDER, resourceId: '1' }))
            .toEqual({ activePane: {}, sliderResourceId: '1' });
    });

    it('закрыть слайдер', () => {
        expect(overlays(defaultState, { type: CLOSE_SLIDER }))
            .toEqual({ activePane: {}, sliderResourceId: '' });
    });

    it('показать топбар мобильного  приложения', () => {
        expect(overlays(defaultState, { type: SET_ACTION_BARS_VISIBILITY, state: true }))
            .toEqual({ activePane: {}, isActionBarsHidden: false });
    });

    it('скрыть топбар мобильного  приложения', () => {
        expect(overlays(defaultState, { type: SET_ACTION_BARS_VISIBILITY, state: false }))
            .toEqual({ activePane: {}, isActionBarsHidden: true });
    });
});
