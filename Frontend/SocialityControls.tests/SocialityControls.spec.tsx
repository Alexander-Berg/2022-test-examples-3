import * as React from 'react';
import { mount, ReactWrapper } from 'enzyme';
import { act } from 'react-dom/test-utils';
import { Reducer } from 'redux';
import { getStore, registerReducer, resetStore as coreResetStore } from '@yandex-turbo/core/state/store';
import { socialityReducer } from '@yandex-turbo/core/state/sociality/reducer';
import { setCommentsCount, commentatorReady } from '@yandex-turbo/core/state/sociality/commentator/actions';
import { setUGCState } from '@yandex-turbo/core/state/sociality/likes/actions';
import { focusCommentatorOrRedirect } from '@yandex-turbo/core/sociality/commentatorHelpers';
import { ReactionsAPI } from '@yandex-turbo/core/api/reactions/reactionsApi';

import { SocialityControls } from '../SocialityControls';
import { EControlStatuses } from '../SocialityControls.types';

jest.mock('@yandex-turbo/core/sociality/commentatorHelpers');
jest.mock('@yandex-turbo/core/api/reactions/reactionsApi', () => {
    return {
        ReactionsAPI: {
            getReaction: jest.fn(() => {
                return Promise.resolve({
                    Aggregate: {
                        LikeCount: '32',
                    },
                    UserReaction: {
                        Like: true,
                    },
                });
            }),

            setReaction: () => Promise.resolve(),

            deleteReaction: () => Promise.resolve(),
        },
    };
});

// TODO: move to utils
type AnyPreloadedState = Record<string, unknown>;
function setPreloadedState(preloadedState: AnyPreloadedState) {
    // @ts-ignore
    window.__GLOBAL_STATE__ = window.__GLOBAL_STATE__ || {};
    // @ts-ignore
    window.__GLOBAL_STATE__.store = preloadedState;
}

function isInStatus(wrapper: ReactWrapper, status: EControlStatuses) {
    const div = wrapper.getDOMNode();

    return div.classList.contains(`turbo-sociality-controls_status_${status}`);
}

function resetStore<S extends AnyPreloadedState>(preloadedState?: S) {
    if (preloadedState) {
        setPreloadedState(preloadedState);
    }

    // @ts-ignore
    window.Ya.store = undefined;
    coreResetStore();
}

function setupStore<S extends AnyPreloadedState>(reducers: Record<string, Reducer>, preloadedState: S) {
    resetStore(preloadedState);

    Object.keys(reducers).forEach(key => {
        registerReducer(key, reducers[key]);
    });
}

function waitFor(wrapper: ReactWrapper, predicate: (wrapper: ReactWrapper) => boolean): Promise<unknown> {
    let attemts = 3;
    const promise = new Promise((resolve, reject) => {
        const check = () => {
            setTimeout(() => {
                wrapper.update();

                if (predicate(wrapper)) {
                    resolve();
                } else if (attemts > 0) {
                    attemts--;
                    check();
                } else {
                    reject('Не дождались успешного предиката');
                }
            }, 0);
        };

        check();
    });

    return expect(promise).resolves.toBeUndefined();
}

const spyOnRafUntil = (factory: () => Promise<unknown>) => {
    let time = 1;
    const spy = jest.spyOn(window, 'requestAnimationFrame')
        .mockImplementation(cb => {
            act(() => cb(time += 600));

            return 0;
        });

    return factory()
        .then(() => {
            spy.mockRestore();
        })
        .catch(e => {
            spy.mockRestore();

            return Promise.reject(e);
        });
};

describe('Блок SocialityControls', () => {
    const initialState = {
        sociality: {
            likes: {
                'page-hash': {
                    fetched: true,
                    entityId: 'test-entity',
                    count: 10,
                    setByUser: false,
                },
            },
            commentator: {},
        },
    };
    beforeAll(() => {
        setupStore(
            {
                sociality: socialityReducer,
            },
            initialState
        );
    });

    afterEach(() => resetStore(initialState));

    test('Рендерится в состоянии загрузки, если данных нет', () => {
        const wrapper = mount(<SocialityControls pageHash="not-existed-page-hash" />);
        const div = wrapper.getDOMNode();

        expect(div.classList.contains('turbo-sociality-controls_status_loading')).toBe(true);
    });

    test('Переходит в состояние dataRecieved если данные есть', () => {
        const wrapper = mount(<SocialityControls pageHash="page-hash" />);
        expect(isInStatus(wrapper, EControlStatuses.dataRecieved)).toBe(true);
    });

    describe('Лайки', () => {
        // TODO: тесты на клиентское получение.
        test('Переходит в состояние idle, после dataRecieved, при получении данных от лайков', async() => {
            const store = getStore();

            store.dispatch(
                // @ts-ignore - симулируем отсутствие данных
                setUGCState({ pageHash: 'page-hash', count: undefined, setByUser: false })
            );

            const wrapper = mount(<SocialityControls pageHash="page-hash" />);

            await waitFor(wrapper, wrapper => isInStatus(wrapper, EControlStatuses.loading));

            store.dispatch(
                setUGCState({ pageHash: 'page-hash', count: 12, setByUser: false })
            );

            await waitFor(wrapper, wrapper => isInStatus(wrapper, EControlStatuses.dataRecieved));

            await spyOnRafUntil(() =>
                waitFor(wrapper, wrapper => isInStatus(wrapper, EControlStatuses.idle)));
        });

        test('Переходит из состояния idle в dataRecieved при получении новых данных от лайков', async() => {
            const wrapper = mount(<SocialityControls pageHash="page-hash" />);

            await spyOnRafUntil(() =>
                waitFor(wrapper, wrapper => isInStatus(wrapper, EControlStatuses.idle)));

            const store = getStore();
            store.dispatch(
                setUGCState({ pageHash: 'page-hash', count: 12, setByUser: false })
            );

            await waitFor(wrapper, wrapper => isInStatus(wrapper, EControlStatuses.dataRecieved));
        });

        test('Не меняет состояния, если лайк был установлен/снят пользователем', async() => {
            const wrapper = mount(<SocialityControls pageHash="page-hash" />);

            await spyOnRafUntil(() =>
                waitFor(wrapper, wrapper => isInStatus(wrapper, EControlStatuses.idle)));

            const btn = wrapper.find('button.turbo-sociality-controls__control_type_like');
            btn.simulate('click');

            await expect(waitFor(wrapper, wrapper => isInStatus(wrapper, EControlStatuses.dataRecieved)))
                .rejects.toBeInstanceOf(Error);

            expect(isInStatus(wrapper, EControlStatuses.idle)).toBe(true);
            expect(btn.text()).toEqual('11');

            btn.simulate('click');

            await expect(waitFor(wrapper, wrapper => isInStatus(wrapper, EControlStatuses.dataRecieved)))
                .rejects.toBeInstanceOf(Error);

            expect(isInStatus(wrapper, EControlStatuses.idle)).toBe(true);
            expect(btn.text()).toEqual('10');
        });

        test('Меняет состояние, если состояние лайка стало известно из данных', async() => {
            const store = getStore();
            store.dispatch(
                // @ts-ignore - симулируем отсутствие данных
                setUGCState({ pageHash: 'page-hash', count: undefined, setByUser: undefined })
            );
            const wrapper = mount(<SocialityControls pageHash="page-hash" />);

            await waitFor(wrapper, wrapper => isInStatus(wrapper, EControlStatuses.loading));

            store.dispatch(
                setUGCState({ pageHash: 'page-hash', count: 11, setByUser: true })
            );

            expect(waitFor(wrapper, wrapper => isInStatus(wrapper, EControlStatuses.dataRecieved)));
        });

        test('Работает с клиентским походом за лайками', async() => {
            resetStore({
                sociality: {
                    likes: {
                        'page-hash': {
                            fetched: false,
                            entityId: 'test-entity',
                        },
                    },
                    commentator: {},
                },
            });

            const wrapper = mount(<SocialityControls pageHash="page-hash" />);

            expect(ReactionsAPI.getReaction).toBeCalledWith('test-entity');

            await spyOnRafUntil(() =>
                waitFor(wrapper, wrapper => isInStatus(wrapper, EControlStatuses.idle)));

            const btn = wrapper.find('button.turbo-sociality-controls__control_type_like');
            expect(btn.hasClass('turbo-social-button_active'));
            expect(btn.text()).toEqual('32');
        });
    });

    describe('Комментарии', () => {
        beforeEach(() => {
            const store = getStore();
            store.dispatch(
                commentatorReady({
                    pageHash: 'page-hash',
                    apiKey: 'apikey',
                    containerId: 'containerId',
                    entityId: 'entityId' })
            );
        });

        test('Переходит в состояние idle, после dataRecieved, при получении данных от комментатора', async() => {
            // Подчищаем лайки, чтобы не мешались
            const store = getStore();
            store.dispatch(
                // @ts-ignore - симулируем отсутствие данных
                setUGCState({ pageHash: 'page-hash', count: undefined, setByUser: false })
            );

            const wrapper = mount(<SocialityControls pageHash="page-hash" />);

            await waitFor(wrapper, wrapper => isInStatus(wrapper, EControlStatuses.loading));

            store.dispatch(
                setCommentsCount({ pageHash: 'page-hash', commentsCount: 12 })
            );

            await waitFor(wrapper, wrapper => isInStatus(wrapper, EControlStatuses.dataRecieved));

            await spyOnRafUntil(() =>
                waitFor(wrapper, wrapper => isInStatus(wrapper, EControlStatuses.idle)));

            const btn = wrapper.find('button.turbo-sociality-controls__control_type_comments');
            expect(btn.text()).toEqual('12');
        });

        test('Переходит из состояния idle в dataRecieved при получении новых данных от комментатора', async() => {
            const wrapper = mount(<SocialityControls pageHash="page-hash" />);

            await spyOnRafUntil(() =>
                waitFor(wrapper, wrapper => isInStatus(wrapper, EControlStatuses.idle)));

            const store = getStore();
            store.dispatch(
                setCommentsCount({ pageHash: 'page-hash', commentsCount: 12 })
            );

            await waitFor(wrapper, wrapper => isInStatus(wrapper, EControlStatuses.dataRecieved));
        });

        test('Вызывает утилиту редиректа и фокуса при клике в комментарии', async() => {
            const store = getStore();
            store.dispatch(
                setCommentsCount({ pageHash: 'page-hash', commentsCount: 12 })
            );

            const wrapper = mount(<SocialityControls pageHash="page-hash" />);

            await spyOnRafUntil(() =>
                waitFor(wrapper, wrapper => isInStatus(wrapper, EControlStatuses.idle)));

            const btn = wrapper.find('button.turbo-sociality-controls__control_type_comments');
            btn.simulate('click');

            expect(focusCommentatorOrRedirect).toBeCalled();
        });
    });
});
