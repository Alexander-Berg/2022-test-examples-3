import { assert } from 'chai';

import { Baobab, INodeContext } from '../Baobab';
import { ISendNode, SpySenderForTest } from '../../Sender/SpySenderForTest';
import { withDynamic, IWithDynamic, IDynamicNode, IDynamicRootNode } from './Baobab_dynamic';
import { INode, INodeInfo, IRootNodeInfo, ISsrNodeInfo } from '../Baobab.typings/Baobab';

const DynamicLogger = withDynamic(Baobab);
let logger: IWithDynamic<INodeContext>;
let sender: SpySenderForTest;
const childInfo: INodeInfo = {
    name: 'child',
    attrs: { some: 'data', other: 'value' },
};
const rootInfo: IRootNodeInfo = {
    name: 'root',
    attrs: { ui: 'NodeJS', service: 'test' },
};
const rootSendNode: ISendNode = {
    id: 'bid-0',
    name: 'root',
    attrs: { 'schema-ver': 0, service: 'test', ui: 'NodeJS' },
    children: [
        { id: 'bid-1', name: 'child', attrs: { other: 'value', some: 'data' } },
    ],
};
const rootSendNodeWithNotAttachedChild: ISendNode = {
    id: 'bid-0',
    name: 'root',
    attrs: { 'schema-ver': 0, service: 'test', ui: 'NodeJS' },
    children: [
        {
            id: 'bid-1',
            name: 'child',
            attrs: { other: 'value', some: 'data' },
        },
    ],
};
const subresultSendNode: ISendNode = {
    id: 'bid-3',
    name: '$subresult',
    attrs: {
        'main-search': false,
        'parent-id': 'bid-1',
        'schema-ver': 0,
        subservice: undefined,
        'trigger-event-id': undefined,
        'trigger-event-trusted': false,
        ui: 'NodeJS',
    },
    children: [
        { id: 'bid-2', name: 'child', attrs: { other: 'value', some: 'data' } },
        { id: 'bid-4', name: 'child', attrs: { other: 'value', some: 'data' } },
    ],
};
const restoreInfo: ISsrNodeInfo<INodeContext> = {
    id: 'r-root',
    context: { ui: 'NodeJS', service: 'test', genInfo: { prefix: 'rid-', nextNodeId: 2 } },
};

describe('Dynamic Baobab', () => {
    let rootNode: IDynamicRootNode<INodeContext>;
    let childNode: IDynamicNode<INodeContext>;

    beforeEach(() => {
        logger = new DynamicLogger({
            prefix: 'bid-',
            senderArgs: { hrefPrefix: '', hrefPostfix: '' },
            Sender: SpySenderForTest,
        });

        sender = logger.getSender() as SpySenderForTest;
        rootNode = logger.createRoot(rootInfo);
        childNode = logger.createChild(rootNode, childInfo);
    });

    it('create root node', () => {
        // Пересоздаём тут отдельно от beforeEach, чтобы проверить на пустые children
        rootNode = logger.createRoot(rootInfo);

        assert.equal(rootNode.child, null, 'Node should be empty after create');
        assert.deepEqual(rootNode.flags, {
            attached: false,
            logged: false,
            restored: false,
        }, 'Node state is wrong after create');

        assert.equal(0, sender.createTree.callCount);
        assert.equal(0, sender.appendTree.callCount);
        assert.equal(0, sender.clientAction.callCount);
        assert.equal(0, sender.attachTree.callCount);
        assert.equal(0, sender.detachTree.callCount);
    });

    it('create child node', () => {
        assert.equal(rootNode.child, childNode, 'Child node is not append to parent node');
        assert.equal(rootNode.child && rootNode.child.next, null, 'One child was add only');
        assert.equal(rootNode, childNode.parent, 'Parent node is not set in child node');
        assert.equal(childNode.name, childInfo.name, 'Name is not correct');
        assert.deepEqual(childNode.attrs, childInfo.attrs, 'Attrs are not correct');
        assert.equal(childNode.child, null, 'Node should be empty after create');
        assert.deepEqual(childNode.flags, {
            logged: false,
            attached: false,
            restored: false,
        }, 'Child node state is wrong after create');

        assert.equal(0, sender.createTree.callCount);
        assert.equal(0, sender.appendTree.callCount);
        assert.equal(0, sender.clientAction.callCount);
        assert.equal(0, sender.attachTree.callCount);
        assert.equal(0, sender.detachTree.callCount);
    });

    it('show & create after first show', () => {
        logger.attachNode(childNode);
        logger.attachNode(rootNode);

        assert.deepEqual(rootNode.flags, {
            logged: true,
            attached: true,
            restored: false,
        }, 'Root node state is wrong after attach');
        assert.deepEqual(childNode.flags, {
            logged: true,
            attached: true,
            restored: false,
        }, 'Child node state is wrong after attach');

        assert.equal(1, sender.createTree.callCount);
        // @ts-ignore
        assert.deepEqual(rootSendNode, sender.createTree.args[0][0].node);
        assert.equal(0, sender.appendTree.callCount);
        assert.equal(0, sender.clientAction.callCount);
        assert.equal(0, sender.attachTree.callCount);
        assert.equal(0, sender.detachTree.callCount);
    });

    it('show & create after first show root without child', () => {
        logger.attachNode(rootNode);

        assert.deepEqual(rootNode.flags, {
            logged: true,
            attached: true,
            restored: false,
        }, 'Root node state is wrong after attach');
        assert.deepEqual(childNode.flags, {
            logged: true,
            attached: false,
            restored: false,
        }, 'Child node state is wrong after attach');

        assert.equal(1, sender.createTree.callCount);
        // @ts-ignore
        assert.deepEqual(rootSendNodeWithNotAttachedChild, sender.createTree.args[0][0].node);
        assert.equal(0, sender.appendTree.callCount);
        assert.equal(0, sender.clientAction.callCount);
        assert.equal(0, sender.attachTree.callCount);
        assert.equal(0, sender.detachTree.callCount);
    });

    it('hide & show again', () => {
        logger.attachNode(childNode);
        logger.attachNode(rootNode);

        logger.detachNode(rootNode);

        assert.deepEqual(rootNode.flags, {
            logged: true,
            attached: false,
            restored: false,
        }, 'Root node state is wrong after detach');
        assert.deepEqual(childNode.flags, {
            logged: true,
            attached: false,
            restored: false,
        }, 'Child node state is wrong after detach');

        logger.attachNode(rootNode);

        assert.deepEqual(rootNode.flags, {
            logged: true,
            attached: true,
            restored: false,
        }, 'Root node state is wrong after second attach');
        assert.deepEqual(childNode.flags, {
            logged: true,
            attached: false,
            restored: false,
        }, 'Child node state is wrong after second attach');

        assert.equal(1, sender.createTree.callCount);
        assert.equal(0, sender.appendTree.callCount);
        assert.equal(0, sender.clientAction.callCount);
        assert.equal(1, sender.attachTree.callCount);
        assert.equal(1, sender.detachTree.callCount);
    });

    it('not show child if parent is hide', () => {
        logger.attachNode(childNode);

        assert.deepEqual(rootNode.flags, {
            logged: false,
            attached: false,
            restored: false,
        }, 'Root node state is wrong after second attach');
        assert.deepEqual(childNode.flags, {
            logged: false,
            attached: true,
            restored: false,
        }, 'Child node state is wrong after second attach');

        assert.equal(0, sender.createTree.callCount);
        assert.equal(0, sender.appendTree.callCount);
        assert.equal(0, sender.clientAction.callCount);
        assert.equal(0, sender.attachTree.callCount);
        assert.equal(0, sender.detachTree.callCount);
    });

    it('create other node for same nodeData in same parent', () => {
        logger.attachNode(childNode);
        logger.attachNode(rootNode);

        const newNode = logger.createChild(rootNode, childInfo);

        assert.notEqual(newNode, childNode, 'Node is the same');
        logger.attachNode(newNode);

        return new Promise(resolve => setTimeout(resolve, 10))
            .then(() => {
                assert.deepEqual(newNode.flags, {
                    logged: true,
                    attached: true,
                    restored: false,
                }, 'New node state is wrong after attach');

                assert.equal(1, sender.createTree.callCount);
                assert.equal(1, sender.appendTree.callCount);
                assert.equal(0, sender.clientAction.callCount);
                assert.equal(0, sender.attachTree.callCount);
                assert.equal(0, sender.detachTree.callCount);
            });
    });

    it('create 2 nodes with one $subresult for one parent', async() => {
        logger.attachNode(childNode);
        logger.attachNode(rootNode);

        return new Promise(resolve => setTimeout(resolve, 10))
            .then(() => {
                const node0 = logger.createChild(childNode, childInfo);
                const node1 = logger.createChild(childNode, childInfo);

                logger.attachNode(node0);
                logger.attachNode(node1);

                return new Promise(resolve => setTimeout(resolve, 10));
            })
            .then(() => {
                assert.equal(1, sender.createTree.callCount, 'sender.createTree call count');
                assert.equal(1, sender.appendTree.callCount, 'sender.appendTree call count');
                // @ts-ignore
                assert.deepEqual(subresultSendNode, sender.appendTree.args[0][0].node);
                assert.equal(0, sender.clientAction.callCount, 'sender.clientAction call count');
                assert.equal(0, sender.attachTree.callCount, 'sender.attachTree call count');
                assert.equal(0, sender.detachTree.callCount, 'sender.detachTree call count');
            });
    });

    it('recreate new node for same nodeData in same parent', () => {
        const newNode = logger.createChild(rootNode, childInfo);
        let restoredNode: INode<INodeContext>;
        let restoredNewNode: INode<INodeContext>;

        logger.attachNode(childNode);
        logger.attachNode(newNode);
        logger.attachNode(rootNode);

        return new Promise(resolve => setTimeout(resolve, 10))
            .then(() => {
                logger.detachNode(childNode);
                logger.detachNode(newNode);

                restoredNode = logger.createChild(rootNode, childInfo);
                restoredNewNode = logger.createChild(rootNode, childInfo);

                logger.attachNode(childNode);
                logger.attachNode(newNode);

                return new Promise(resolve => setTimeout(resolve, 10));
            })
            .then(() => {
                assert.notEqual(restoredNode, childNode, 'Node is the same');
                assert.notEqual(restoredNewNode, newNode, 'New node is the same');

                assert.equal(sender.createTree.callCount, 1);
                assert.equal(sender.appendTree.callCount, 0);
                assert.equal(sender.clientAction.callCount, 0);
                assert.equal(sender.attachTree.callCount, 2);
                assert.equal(sender.detachTree.callCount, 2);
            });
    });

    describe('restore node in tree', () => {
        it('not logged node', () => {
            const node = logger.restoreNode(restoreInfo);

            assert.deepEqual(
                node.flags,
                { restored: true, logged: false, attached: false },
                'Node should be created with correct flags',
            );
        });
        it('logged node', () => {
            const node = logger.restoreNode(restoreInfo, true);

            assert.deepEqual(
                node.flags,
                { restored: true, logged: true, attached: true },
                'Node should be created with correct flags',
            );
        });
    });
});
