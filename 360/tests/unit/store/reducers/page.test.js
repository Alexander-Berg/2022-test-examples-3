import page from '../../../../components/redux/store/reducers/page';
import { MOVE_CLONED_RESOURCES } from '../../../../components/redux/store/actions/types';
import deepFreeze from 'deep-freeze';

describe('page reducer', () => {
    describe('MOVE_CLONED_RESOURCE', () => {
        it('Должен обновить idDialog если передали newIdDialog', () => {
            const state = {
                dialog: 'slider',
                idDialog: '/disk/2'
            };
            deepFreeze(state);
            const newState = page(state, {
                type: MOVE_CLONED_RESOURCES,
                payload: {
                    idContext: '/remember/id',
                    resources: [{
                        src: { id: '/disk/2' },
                        dst: { id: '/disk/2__' }
                    }],
                    newIdDialog: '/disk/2__'
                }
            });
            expect(newState.idDialog).toBe('/disk/2__');
        });
        it('Не должен обновить idDialog если в слайдере открыт не src', () => {
            const state = {
                dialog: 'slider',
                idDialog: '/disk/3'
            };
            deepFreeze(state);
            const newState = page(state, {
                type: MOVE_CLONED_RESOURCES,
                payload: {
                    idContext: '/remember/id',
                    resources: [{
                        src: { id: '/disk/1' },
                        dst: { id: '/disk/1__' }
                    }, {
                        src: { id: '/disk/2' },
                        dst: { id: '/disk/2__' }
                    }]
                }
            });
            expect(newState.idDialog).toBe('/disk/3');
        });
    });
});
