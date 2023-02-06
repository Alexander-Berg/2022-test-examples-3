import '../../noscript';
import _ from 'lodash';
import createStore from '../../../../components/redux/store/create-store';
import { moveSlider } from '../../../../components/redux/store/actions/slider';

describe('slider actions', () => {
    describe('moveSlider', () => {
        const createTestStore = (state) => (createStore(_.merge({
            user: { sids: [] },
            page: {
                idContext: '/remember/id'
            },
            environment: { agent: { isMobile: true }, session: { experiment: {} } },
            resources: {
                '/remember/id': {
                    id: '/remember/id',
                    children: ['/disk/one', '/disk/two', '/disk/three']
                },
                '/disk': {
                    id: '/disk',
                    children: ['/disk/one', '/disk/two', '/disk/three']
                },
                '/disk/one': {
                    id: '/disk/one',
                    type: 'file',
                    state: {},
                    meta: {}
                },
                '/disk/two': {
                    id: '/disk/two',
                    type: 'file',
                    state: { hidden: true },
                    meta: {}
                },
                '/disk/three': {
                    id: '/disk/three',
                    type: 'file',
                    state: {},
                    meta: {}
                }
            }
        }, state)));

        beforeEach(() => {
            ns.action.run = jest.fn(() => {});
        });

        it('переключение слайдера в блоке воспоминаний', () => {
            const store = createTestStore({ page: { idDialog: '/disk/one' } });
            store.dispatch(moveSlider(1));
            expect(ns.action.run).toBeCalledWith('app.openSlider', { id: '/disk/three' });
        });

        it('слайдер должен закрываться при перелистывании вперёд на последнем ресурсе', () => {
            const store = createTestStore({ page: { idDialog: '/disk/three' } });
            store.dispatch(moveSlider(1));
            expect(ns.action.run).toBeCalledWith('dialog.close');
        });
    });
});
