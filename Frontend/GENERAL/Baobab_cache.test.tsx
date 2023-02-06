import { assert } from 'chai';

import { SpySenderForTest } from '../../Sender/SpySenderForTest';
import { Baobab } from '../Baobab';
import { INodeContext } from '../Baobab.typings/Baobab';
import { IWithDynamic, withDynamic } from '../_dynamic/Baobab_dynamic';

import { withCache } from './Baobab_cache';

const DynamicLogger = withDynamic<INodeContext, typeof Baobab>(Baobab);
const CacheLogger = withCache<INodeContext, typeof DynamicLogger>(DynamicLogger);

describe('Cache Baobab', () => {
    let logger: IWithDynamic<INodeContext>;
    let sender: SpySenderForTest;

    beforeEach(() => {
        logger = new CacheLogger({
            prefix: 'bid-',
            senderArgs: { hrefPrefix: '', hrefPostfix: '' },
            Sender: SpySenderForTest,
        });
        sender = logger.getSender() as SpySenderForTest;
    });

    it('should recreate the same node for same nodeData in same parent', () => {
        const rootData = { name: 'root', attrs: { ui: 'NodeJS', service: 'test' } };
        const nodeData = { name: 'child', attrs: { some: 'data' } };

        const root = logger.createRoot(rootData);
        const node = logger.createChild(root, nodeData);
        const newNode = logger.createChild(root, nodeData);

        logger.attachNode(node);
        logger.attachNode(newNode);
        logger.attachNode(root);

        logger.detachNode(node);
        logger.detachNode(newNode);

        const restoredNode = logger.createChild(root, nodeData);
        const restoredNewNode = logger.createChild(root, nodeData);

        logger.attachNode(node);
        logger.attachNode(newNode);

        assert.equal(restoredNode, node, 'Node is not the same');
        assert.equal(restoredNewNode, newNode, 'New node is not the same');

        assert.equal(1, sender.createTree.callCount);
        assert.equal(0, sender.appendTree.callCount);
        assert.equal(0, sender.clientAction.callCount);
        assert.equal(2, sender.attachTree.callCount);
        assert.equal(2, sender.detachTree.callCount);
    });
});
