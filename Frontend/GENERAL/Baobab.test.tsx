import { assert } from 'chai';

import { Baobab } from './Baobab';
import {
    IBaobab,
    INodeContext,
    INodeInfo,
    IRootNodeInfo,
    ISsrNodeInfo,
} from './Baobab.typings/Baobab';

let logger: IBaobab<INodeContext>;

const childInfo: INodeInfo = {
    name: 'child',
    attrs: { some: 'data', other: 'value' },
};

const rootInfo: IRootNodeInfo = {
    name: 'root',
    attrs: { ui: 'NodeJS', service: 'test' },
};

const restoreInfo: ISsrNodeInfo<INodeContext> = {
    id: 'r-root',
    context: { ui: 'NodeJS', service: 'test', genInfo: { prefix: 'rid-', nextNodeId: 2 } },
};

const rootNodeAtts = { ... rootInfo.attrs, 'schema-ver': 0 };

describe('Baobab', () => {
    beforeEach(() => {
        logger = new Baobab({
            prefix: 'bid-',
        });
    });

    describe('create', () => {
        it('root node', () => {
            const root = logger.createRoot(rootInfo);

            assert.equal(root.name, rootInfo.name, 'Name is not correct');
            assert.deepEqual(root.attrs, rootNodeAtts, 'Attrs are not correct');
            assert.equal(root.child, null, 'Node should be empty after create');
        });

        it('child node', () => {
            const root = logger.createRoot(rootInfo);
            const node = logger.createChild(root, childInfo);

            assert.equal(root.child, node, 'Child node is not append to parent node');
            assert.equal(root.child && root.child.next, null, 'One child was add only');
            assert.equal(root, node.parent, 'Parent node is not set in child node');
            assert.equal(node.name, childInfo.name, 'Name is not correct');
            assert.deepEqual(node.attrs, childInfo.attrs, 'Attrs are not correct');
            assert.equal(node.child, null, 'Node should be empty after create');
        });

        it('send node', () => {
            const root = logger.createRoot(rootInfo);
            logger.createChild(root, childInfo);
            const sendNode = logger.createSendNode(root);

            assert.deepEqual(sendNode, {
                id: 'bid-0',
                name: rootInfo.name,
                attrs: rootNodeAtts,
                children: [{
                    id: 'bid-1',
                    name: childInfo.name,
                    attrs: childInfo.attrs,
                }],
            }, 'Send node is not correct');
        });
    });

    describe('restore root node in tree', () => {
        it('root node', () => {
            const root = logger.restoreNode(restoreInfo);

            assert.equal(root.name, '', 'Name is not correct');
            assert.equal(root.id, restoreInfo.id, 'Id is not correct');
            assert.equal(root.attrs, undefined, 'Attrs are not correct');
            assert.equal(root.child, null, 'Node should be empty after create');
        });

        it('child node', () => {
            const root = logger.restoreNode(restoreInfo);
            const node = logger.createChild(root, childInfo);

            assert.equal(root.child, node, 'Child node is not append to parent node');
            assert.equal(root.child && root.child.next, null, 'One child was add only');
            assert.equal(root, node.parent, 'Parent node is not set in child node');
            assert.equal(node.name, childInfo.name, 'Name is not correct');
            assert.equal(node.id, 'rid-2', 'Id is not correct');
            assert.deepEqual(node.attrs, childInfo.attrs, 'Attrs are not correct');
            assert.equal(node.child, null, 'Node should be empty after create');
        });
    });
});
