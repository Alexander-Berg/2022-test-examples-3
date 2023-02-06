import { assert } from 'chai';
import { INodeInfo, IRootNodeInfo } from '../common/Baobab/Baobab';
import { areNotEqualsNodes } from './areNotEqualsNodes';
import { createNodeInfo } from './createNodeInfo';

const rootDeclaration: IRootNodeInfo = {
    name: 'root',
    attrs: { ui: 'NodeJS', service: 'test' },
};

const childDeclaration: INodeInfo = {
    name: 'child',
    attrs: { some: 'data', other: 'value' },
};
const childLogNode: INodeInfo = {
    name: 'new-name',
    attrs: { other: 'value_2', someMore: 'mode-data' },
};

describe('createNodeData', () => {
    it('for node', () => {
        const rootData = createNodeInfo(rootDeclaration);

        assert.isFalse(areNotEqualsNodes(rootDeclaration, rootData));
    });

    it('for node with other data', () => {
        const nodeData1 = createNodeInfo(childDeclaration);
        const nodeData2 = createNodeInfo(childDeclaration, childLogNode, {});

        assert.isTrue(areNotEqualsNodes(nodeData1, nodeData2));
    });
});
