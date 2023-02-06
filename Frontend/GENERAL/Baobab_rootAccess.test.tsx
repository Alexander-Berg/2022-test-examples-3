import { assert } from 'chai';

import { Baobab, IBaobab, IRootNode } from '../Baobab';
import { INodeContext } from '../Baobab.typings/Baobab';
import { withRootAccess, IWithRootAccess } from './Baobab_rootAccess';

describe('Root access Baobab', () => {
    const RootAccessLogger = withRootAccess(Baobab);
    let logger: IWithRootAccess<INodeContext> & IBaobab<INodeContext>;
    let rootNode: IRootNode<INodeContext>;

    beforeEach(() => {
        logger = new RootAccessLogger({ prefix: 'bid-' });
        rootNode = logger.createRoot({ name: 'root', attrs: { ui: 'NodeJS', service: 'test' } });
    });

    it('should get root node', () => {
        const gettedRoot = logger.getRootNode();

        assert.equal(gettedRoot, rootNode, 'Root node should the same');
    });

    it('should get send root node', () => {
        const gettedSendRoot = logger.getRootSendNode();
        const encodedRoot = logger.createSendNode(rootNode);

        assert.deepEqual(gettedSendRoot, encodedRoot, 'Encode root node should the same');
    });
});
