import { showStatus, hideStatus } from '../actions';
import { statusScreensReducer, initialState } from '../reducer';

describe('statusScreensReducer', () => {
    it('должен инитится дефолтным стейтом', () => {
        expect(
            statusScreensReducer(undefined, { type: '@INIT' }),
            'стейт проинициализировался не initialState'
        ).toEqual(initialState);
    });

    it('должен вернуть прежний объект', () => {
        expect(
            statusScreensReducer(initialState, { type: '@RANDOM' }),
            'при рандомном редьюсере вернулся новый объект'
        )
            .toBe(initialState);
    });

    it('должен возвращать новый объект', () => {
        expect(
            statusScreensReducer(initialState, hideStatus()),
            'редьюсер мутировал старое состояние или вернул тот же state'
        ).not.toBe(initialState);
    });

    it('должен удалить данные (hideStatus)', () => {
        expect(
            statusScreensReducer({
                status: 'FOO_STATUS',
            }, hideStatus()),
            'не обнулилось состояние'
        ).toEqual({});
    });

    it('должен записать данные (showStatus)', () => {
        expect(
            statusScreensReducer({
                data: { foo: 1 },
                status: 'FOO_STATUS',
            }, showStatus('BAR_STATUS')),
            'не обновилось состояние'
        ).toEqual({
            data: {},
            status: 'BAR_STATUS',
        });
        expect(
            statusScreensReducer({
                data: { foo: 1 },
                status: 'FOO_STATUS',
            }, showStatus('BAR_STATUS', { bar: 1 })),
            'не обновилось состояние'
        ).toEqual({
            data: { bar: 1 },
            status: 'BAR_STATUS',
        });
    });
});
