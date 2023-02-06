import actionParser, { ActionTypes } from '../actionParser';
import { EventType } from '../../typings/assistant';

describe('Dialog URI', () => {
    describe('#actionParser', () => {
        const directives = {
            foo: {
                type: EventType.TEXT_INPUT,
                text: 'foo',
            },
            bar: {
                type: EventType.TEXT_INPUT,
                text: 'bar',
            },
        };

        it('For { uri: dialog-action:?directives=[{foo},{bar}] } should return { type: dialog-action }', () => {
            const directivesValue = JSON.stringify([directives.foo, directives.bar]);

            expect(actionParser(`dialog-action:?directives=${directivesValue}`)).toEqual({
                type: ActionTypes.DIALOG_ACTION,
                directives: [directives.foo, directives.bar],
            });
        });

        it('For { uri: dialog-action:?directives=[] } should return undefined', () => {
            expect(actionParser('dialog-action:?directives=[]')).toEqual(undefined);
        });

        it('For { uri: dialog-action:?directives={foo} } should return undefined', () => {
            const directivesValue = JSON.stringify(directives.foo);

            expect(actionParser(`dialog-action:?directives=${directivesValue}`)).toEqual(undefined);
        });

        it('For { uri: dialog-action:?directives=text } should return undefined', () => {
            expect(actionParser('dialog-action:?directives=text')).toEqual(undefined);
        });

        it('For { uri: dialog-action: } should return undefined', () => {
            expect(actionParser('dialog-action:')).toEqual(undefined);
        });

        it('For { uri: div-action://set_state?state_id=2 } should return { type: div-action }', () => {
            expect(actionParser('div-action://set_state?state_id=2')).toEqual({
                type: ActionTypes.DIV,
                method: 'set_state',
                params: {
                    stateId: 2,
                },
            });
        });

        it('For { uri: div-action://unknown_method?foo=bar } should return undefined', () => {
            expect(actionParser('div-action://unknown_method?foo=bar')).toEqual(undefined);
        });

        it('For { uri: http://ya.ru should return { type: uri }', () => {
            expect(actionParser('http://ya.ru')).toEqual({
                type: ActionTypes.URI,
                uri: 'http://ya.ru',
            });
        });

        it('For { uri: https://dialogs.yandex.ru/store/skills/7fa868e8-goroda } should return { type: uri }', () => {
            expect(actionParser('https://dialogs.yandex.ru/store/skills/7fa868e8-goroda')).toEqual({
                type: ActionTypes.URI,
                uri: 'https://dialogs.yandex.ru/store/skills/7fa868e8-goroda',
            });
        });

        it('For { uri: opensettings://?screen=feed } should return undefined', () => {
            expect(actionParser('opensettings://?screen=feed')).toEqual(undefined);
        });

        it('For undefined should return undefined', () => {
            expect(actionParser()).toEqual(undefined);
        });
    });
});
