import React from 'react';
import { mount } from 'enzyme';

import { TreeContainer } from './Tree.container';

jest.mock('./Tree');
jest.mock('~/src/common/components/WikiFormatter');

describe('Should fetch data', () => {
    it('upon init, should not dispatch fetch action if url is empty', () => {
        const updateRequest = jest.fn();

        const wrapper = mount(
            <TreeContainer
                requestUpdate={updateRequest}
                requestAllNodeChildren={jest.fn()}
                requestNodeChildrenUpdate={jest.fn()}
                setNodeOpened={jest.fn()}
                setNodeClosed={jest.fn()}
                filters={{}}
                tree={{}}
                error={null}
                status="inited"
                rootServiceId={0}
                isUrlEmpty
            />
        );

        expect(updateRequest).not.toHaveBeenCalled();

        wrapper.unmount();
    });

    it('upon init, should dispatch fetch action if url is not empty', () => {
        const updateRequest = jest.fn();

        const wrapper = mount(
            <TreeContainer
                requestUpdate={updateRequest}
                requestAllNodeChildren={jest.fn()}
                requestNodeChildrenUpdate={jest.fn()}
                setNodeOpened={jest.fn()}
                setNodeClosed={jest.fn()}
                filters={{}}
                tree={new Map()}
                error={null}
                status="inited"
                rootServiceId={0}
                isUrlEmpty={false}
            />
        );

        expect(updateRequest).toHaveBeenCalled();

        wrapper.unmount();
    });

    it('upon request filters update, debounced 1000ms', done => {
        const updateRequest = jest.fn();

        const wrapper = mount(
            <TreeContainer
                requestUpdate={updateRequest}
                requestAllNodeChildren={jest.fn()}
                requestNodeChildrenUpdate={jest.fn()}
                setNodeOpened={jest.fn()}
                setNodeClosed={jest.fn()}
                filters={{}}
                tree={{}}
                error={null}
                status="inited"
                rootServiceId={0}
            />
        );

        const initialCalls = updateRequest.mock.calls.length;

        wrapper.setProps({ filters: { foo: 'bar' } });

        // fake timers не работают из коробки с lodash debounce, нужно мокать чуть сложнее
        // https://github.com/facebook/jest/issues/3465#issuecomment-504908570
        setTimeout(() => {
            expect(updateRequest).toHaveBeenCalledTimes(initialCalls + 1);
            wrapper.unmount();
            done();
        }, 1000);
    });
});

describe('Should prepare data', () => {
    it('for the view component', () => {
        const wrapper = mount(
            <TreeContainer
                requestUpdate={jest.fn()}
                requestAllNodeChildren={jest.fn()}
                requestNodeChildrenUpdate={jest.fn()}
                setNodeOpened={jest.fn()}
                setNodeClosed={jest.fn()}
                isUrlEmpty
                tree={{
                    '0': {
                        isRoot: true,
                        children: [1, 2],
                    },
                    '1': {
                        data: { id: 1, parent: null },
                        childrenDataStatus: 'pending',
                        childrenError: null,
                        isOpen: false,
                        children: [1.1],
                    },
                    '2': {
                        data: { id: 2, parent: null },
                        childrenDataStatus: 'inited',
                        childrenError: null,
                        isOpen: true,
                        children: [2.1, 2.2],
                    },
                    '1.1': {
                        data: { id: 1.1, parent: { id: 1 } },
                        childrenDataStatus: 'pending',
                        childrenError: null,
                        isOpen: false,
                        children: [],
                    },
                    '2.1': {
                        data: { id: 2.1, parent: { id: 2 } },
                        childrenDataStatus: 'pending',
                        childrenError: null,
                        isOpen: false,
                        children: [],
                    },
                    '2.2': {
                        data: { id: 2.2, parent: { id: 2 } },
                        childrenDataStatus: 'pending',
                        childrenError: null,
                        isOpen: false,
                        children: [],
                    },
                }}
            />
        );

        expect(wrapper).toMatchSnapshot();
    });
});
