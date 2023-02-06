import * as React from 'react';
import { shallow, mount } from 'enzyme';
import { Comments } from '@yandex-turbo/components/Comments/Comments';

import { DynamicCommentsComponent, IProps, StateProps, IState } from '../DynamicComments';
import { ShowMore } from '../ShowMore/DynamicComments-ShowMore';
import { getComments } from '../DynamicCommentsApi';
import { initialCommentsHash, fullCommentsHash, paginatedCommentsHash } from '../__mocks__/data';

jest.mock('../DynamicCommentsApi');

const props: IProps & StateProps = {
    dispatch: jest.fn(),
    cancelCommentText: 'cancelCommentText',
    errorSendCommentText: 'errorSendCommentText',
    commentReplyText: 'commentReplyText',
    showMoreText: 'showMoreText',
    showMoreLoadingText: 'showMoreLoadingText',
    sendCommentText: 'sendCommentText',
    newCommentText: 'newCommentText',
    getUrl: 'getUrl',
    addUrl: 'addUrl',
    limit: 1,
    state: {
        loginEndpoint: 'loginEndpoint',
        loginFormVisible: false,
    },
};

const flushPromise = () => Promise.resolve().then();

describe('DynamicCommentsComponent', () => {
    beforeEach(() => {
        jest.clearAllMocks();
    });

    test('Рендерится без ошибок', () => {
        const wrapper = shallow(
            <DynamicCommentsComponent {...props} />
        );

        expect(wrapper.exists());
    });

    test('Запрашивает комментарии и формирует state', async() => {
        const wrapper = shallow<IProps & StateProps, IState>(
            <DynamicCommentsComponent {...props} />
        );
        const expectedState = {
            paginationState: {
                root: {
                    total: 4,
                    offset: 0,
                    loading: false,
                },
                '1': {
                    total: 2,
                    offset: 0,
                    commentIds: ['2'],
                },
            },
            comments: initialCommentsHash,
        };

        // запросили данные на маунте
        expect(getComments).toBeCalledWith('getUrl', { offset: 0, limit: 1 });
        expect(wrapper.state().paginationState.root.loading).toBe(true);

        await flushPromise();

        expect(wrapper.state()).toMatchObject(expectedState);
    });

    test('Запрашивает следующую страницу комментариев', async() => {
        const wrapper = mount(<DynamicCommentsComponent {...props} />);

        await flushPromise();
        wrapper.update();

        wrapper.find(ShowMore).last().find('Link').simulate('click');
        expect(getComments).toBeCalledWith('getUrl', { offset: 1, limit: 1 });

        await flushPromise();
        wrapper.update();

        expect(wrapper.state()).toMatchObject({
            paginationState: {
                root: {
                    total: 4,
                    offset: 1,
                    loading: false,
                },
                '1': {
                    total: 2,
                    offset: 0,
                    commentIds: ['2'],
                },
            },
            comments: fullCommentsHash,
        });
    });

    test('Запрашивает страницу с указаным limit', async() => {
        const wrapper = shallow<IProps & StateProps, IState>(
            <DynamicCommentsComponent {...props} limit={2} />
        );

        expect(getComments).toBeCalledWith('getUrl', { offset: 0, limit: 2 });

        await flushPromise();
        wrapper.update();

        const expectedState = {
            comments: fullCommentsHash,
        };

        expect(wrapper.state()).toMatchObject(expectedState);
    });

    test('Работает клиентская пагинация', async() => {
        const wrapper = mount(<DynamicCommentsComponent {...props} />);

        await flushPromise();
        wrapper.update();

        // реплаи прошли клиентскую пагинацию
        expect(wrapper.find(Comments).prop('comments')).toEqual(paginatedCommentsHash);
        wrapper.find(ShowMore).first().find('Link').simulate('click');
        wrapper.update();

        expect(wrapper.state()).toMatchObject({
            paginationState: {
                '1': {
                    total: 2,
                    offset: 1,
                    commentIds: ['2', '3'],
                },
            },
        });
        expect(wrapper.find(Comments).prop('comments')).toEqual(initialCommentsHash);
        expect(wrapper.find(ShowMore).length).toBe(1);
    });
});
