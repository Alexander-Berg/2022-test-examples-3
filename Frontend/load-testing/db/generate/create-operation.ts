/* eslint-disable */
import { operationTypes } from '../../../db/tables/operation';
import { OperationAttributes } from '../../../db/tables/operation';

interface MOperationAttributes extends OperationAttributes {
    itemId: string;
    type?: string; // skillCreated
}

function generateOperation(props: MOperationAttributes, inum: number) {
    const {
        itemId = 'xxxxxxx',
        type = operationTypes[inum % operationTypes.length], // 'skillCreated'and etc.
    } = props;
    return {
        itemId,
        type,
    };
}

export const generateOperations = async(props: MOperationAttributes, nOperations: number) => {
    const operations = [];
    for (let i = 0; i < nOperations; i++) {
        operations.push(generateOperation(props, i));
    }
    return operations;
};
