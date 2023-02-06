import React from 'react';
import { mount } from 'enzyme';

import { TreeContainer } from './Tree.container';
import { TreeRow } from './Row/Tree-Row';

const permissions = {
    can_filter_and_click: true,
    view_all_services: true,
    view_traffic_light: true,
};

const { BEM_LANG } = process.env;

describe('Handling data fetch', () => {
    it('Should fetch data upon clicking a node', () => {
        let wrapper;

        const tree = {
            '0': {
                isRoot: true,
                children: [1],
            },
            '1': {
                data: { id: 1, parent: null, name: { [BEM_LANG]: 'Сервис 1' }, childrenCount: 10 },
                children: [],
                childrenDataStatus: 'pending',
                childrenError: null,
            },
        };

        const childrenUpdateRequest = jest.fn().mockImplementation(() => {
            tree[1].childrenDataStatus = 'loading';
            wrapper.setProps({ tree: { ...tree } });
        });
        const openNode = jest.fn().mockImplementation(() => {
            tree[1].isOpen = true;
            wrapper.setProps({ tree: { ...tree } });
        });
        const closeNode = jest.fn().mockImplementation(() => {
            tree[1].isOpen = false;
            wrapper.setProps({ tree: { ...tree } });
        });

        wrapper = mount(
            <TreeContainer
                error={null}
                filters={{}}
                permissions={permissions}
                requestAllNodeChildren={jest.fn()}
                requestNodeChildrenUpdate={childrenUpdateRequest}
                requestUpdate={jest.fn()}
                rootServiceId={0}
                setNodeClosed={closeNode}
                setNodeOpened={openNode}
                status="inited"
                tree={tree}
            />
        );

        const initialUpdateCalls = childrenUpdateRequest.mock.calls.length;
        const initialOpenCalls = openNode.mock.calls.length;
        const initialCloseCalls = closeNode.mock.calls.length;

        wrapper.find(TreeRow).at(0).simulate('click');

        // узел должен открыться, данные должны запроситься
        expect(childrenUpdateRequest).toHaveBeenCalledTimes(initialUpdateCalls + 1);
        expect(openNode).toHaveBeenCalledTimes(initialOpenCalls + 1);
        expect(closeNode).toHaveBeenCalledTimes(initialCloseCalls);

        wrapper.find(TreeRow).at(0).simulate('click');

        // узел долен закрыться
        expect(childrenUpdateRequest).toHaveBeenCalledTimes(initialUpdateCalls + 1);
        expect(openNode).toHaveBeenCalledTimes(initialOpenCalls + 1);
        expect(closeNode).toHaveBeenCalledTimes(initialCloseCalls + 1);

        wrapper.find(TreeRow).at(0).simulate('click');

        // узел должен закрыться, данные перезапрашиваться не должны
        expect(childrenUpdateRequest).toHaveBeenCalledTimes(initialUpdateCalls + 1);
        expect(openNode).toHaveBeenCalledTimes(initialOpenCalls + 2);
        expect(closeNode).toHaveBeenCalledTimes(initialCloseCalls + 1);

        wrapper.unmount();
    });

    it('Should not handle clicks on a leaf', () => {
        let wrapper;

        const tree = {
            '0': {
                isRoot: true,
                children: [1],
            },
            '1': {
                data: { id: 1, parent: null, name: { [BEM_LANG]: 'Сервис 1' } },
                children: [],
                childrenDataStatus: 'pending',
                childrenError: null,
            },
        };

        const childrenUpdateRequest = jest.fn();
        const openNode = jest.fn();
        const closeNode = jest.fn();

        wrapper = mount(
            <TreeContainer
                error={null}
                filters={{}}
                permissions={permissions}
                requestAllNodeChildren={jest.fn()}
                requestNodeChildrenUpdate={childrenUpdateRequest}
                requestUpdate={jest.fn()}
                rootServiceId={0}
                setNodeClosed={closeNode}
                setNodeOpened={openNode}
                status="inited"
                tree={tree}
            />
        );

        const initialUpdateCalls = childrenUpdateRequest.mock.calls.length;
        const initialOpenCalls = openNode.mock.calls.length;
        const initialCloseCalls = closeNode.mock.calls.length;

        wrapper.find(TreeRow).at(0).simulate('click');

        // лист не должен обрабатывать клики
        expect(childrenUpdateRequest).toHaveBeenCalledTimes(initialUpdateCalls);
        expect(openNode).toHaveBeenCalledTimes(initialOpenCalls);
        expect(closeNode).toHaveBeenCalledTimes(initialCloseCalls);

        wrapper.unmount();
    });

    it('Should fetch data upon clicking "show all" on a node', () => {
        let wrapper;

        const tree = {
            '0': {
                isRoot: true,
                children: [1],
            },
            '1': {
                data: { id: 1, parent: null, name: { [BEM_LANG]: 'Сервис 1' }, childrenCount: 10 },
                isOpen: true,
                children: [],
                childrenDataStatus: 'inited',
                childrenError: null,
            },
        };

        const allChildrenRequest = jest.fn().mockImplementation(() => {
            tree[1].childrenDataStatus = 'loading';
            wrapper.setProps({ tree: { ...tree } });
        });

        wrapper = mount(
            <TreeContainer
                error={null}
                filters={{}}
                permissions={permissions}
                requestAllNodeChildren={allChildrenRequest}
                requestNodeChildrenUpdate={jest.fn()}
                requestUpdate={jest.fn()}
                rootServiceId={0}
                setNodeClosed={jest.fn()}
                setNodeOpened={jest.fn()}
                status="inited"
                tree={tree}
            />
        );

        const initialUpdateCalls = allChildrenRequest.mock.calls.length;

        wrapper.find('.Tree-ShowUnmatchedButton').at(0).simulate('click');

        expect(allChildrenRequest).toHaveBeenCalledTimes(initialUpdateCalls + 1);

        wrapper.unmount();
    });
});
