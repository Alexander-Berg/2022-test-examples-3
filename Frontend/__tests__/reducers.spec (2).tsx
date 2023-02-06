import { setActiveScreen, setInitialScreen, nextScreen } from '../actions';
import { screensReducer, initialState } from '../reducer';

describe('screensReducer', () => {
    it('должен инитится дефолтным стейтом', () => {
        expect(
            screensReducer(undefined, { type: '@INIT' }),
            'стейт проинициализировался не initialState'
        ).toEqual(initialState);
    });

    it('должен вернуть прежний объект', () => {
        expect(
            screensReducer(initialState, { type: '@RANDOM' }),
            'при рандомном редьюсере вернулся новый объект'
        )
            .toBe(initialState);
    });

    it('должен возвращать новый объект', () => {
        expect(
            screensReducer(initialState, setInitialScreen({
                ...initialState,
            })),
            'редьюсер мутировал старое состояние или вернул тот же state'
        ).not.toBe(initialState);
    });

    it('должен перезаписать состояние (setInitialScreen)', () => {
        expect(
            screensReducer({
                screensArray: ['foo', 'bar', 'baz'],
                activeScreenId: 'foo',
            }, setInitialScreen({
                screensArray: ['newFoo'],
            })),
            'не записалось состояние'
        ).toEqual({ screensArray: ['newFoo'] });
    });

    it('устанавливает нужный screenId (setActiveScreen)', () => {
        const state = screensReducer(initialState, setActiveScreen('foo'));
        expect(
            state,
            'не установился активный скрин с дефолтным pushHustory == true'
        ).toEqual({
            screensArray: [],
            activeScreenId: 'foo',
            screenWithPushHistory: true,
        });

        expect(
            screensReducer(state, setActiveScreen('bar', false)),
            'не установился активный скрин с pushHustory == false'
        ).toEqual({
            screensArray: [],
            activeScreenId: 'bar',
            screenWithPushHistory: false,
        });
    });

    it('должен высчитывать следующий скрин (nextScreen)', () => {
        let state = {
            screensArray: ['foo', 'bar', 'baz'],
            activeScreenId: 'foo',
            screenWithPushHistory: false,
        };
        const startState = state;

        state = screensReducer(state, nextScreen());

        expect(
            state,
            'не переключился скирн'
        ).toEqual({
            ...startState,
            screenWithPushHistory: true,
            activeScreenId: 'bar',
        });

        state = screensReducer(state, nextScreen(false));

        expect(
            state,
            'не переключился скирн с установкой pushBundle = false'
        ).toEqual({
            ...startState,
            screenWithPushHistory: false,
            activeScreenId: 'baz',
        });

        state = screensReducer(state, nextScreen());

        expect(
            state,
            'состояние должно остаться прежним'
        ).toEqual({
            ...startState,
            screenWithPushHistory: false,
            activeScreenId: 'baz',
        });

        expect(
            screensReducer(initialState, nextScreen()),
            'при пустом списке скринов не вернул предыдущее состояние'
        ).toBe(initialState);
    });
});
