import { assert } from 'chai';
import { INodeInfo } from '../common/Baobab/Baobab';
import { createNodeInfo } from './createNodeInfo';

const childDeclaration: INodeInfo = {
    name: 'child',
    attrs: { some: 'data', other: 'value' },
};
const childLogNode: INodeInfo = {
    name: 'new-name',
    attrs: { other: 'value_2', someMore: 'mode-data' },
};

describe('createNodeInfo', () => {
    it('only name', () => {
        const nodeData = createNodeInfo({ name: 'child' });

        assert.deepEqual(nodeData, { name: 'child' });
    });

    it('with data', () => {
        const nodeData = createNodeInfo(childDeclaration);

        assert.deepEqual(nodeData, {
            name: 'child',
            attrs: { some: 'data', other: 'value' },
        });
    });

    it('with logNode', () => {
        const nodeData = createNodeInfo(childDeclaration, childLogNode);

        assert.deepEqual(nodeData, {
            name: 'new-name',
            attrs: { some: 'data', other: 'value_2', someMore: 'mode-data' },
        });
    });
});
