import _ from 'lodash';
import type { ISendNode } from '@yandex-int/react-baobab-logger/lib/common/Sender/Sender.typings/Sender';
import type { INodeAttrs } from '@yandex-int/react-baobab-logger/lib/common/Baobab/Baobab.typings/common';

interface IFindDiffNodesParams {
    server: ISendNode;
    client: ISendNode;
}

const IGNORE_ATTRS = ['oldPath', 'oldVars'];

function compareAttrs(expectedAttrs: INodeAttrs, attrs?: INodeAttrs) {
    const ignoreExpectedKeys = Object
        .keys(expectedAttrs)
        .filter(key => expectedAttrs[key] === '*');

    expectedAttrs = _.omit(expectedAttrs, ignoreExpectedKeys);
    attrs = _.omit(attrs, [...IGNORE_ATTRS, ...ignoreExpectedKeys]);

    return _.isEqual(attrs, expectedAttrs);
}

function areNodesIdentical({ server, client }: IFindDiffNodesParams) {
    return Boolean(server && client && server.id === client.id && server.name === client.name);
}

/** Найти узел в объекте по пути path */
export function query(
    path: string,
    parentNode: ISendNode,
    attrs?: INodeAttrs,
): ISendNode[] {
    const parts = path.split('.');
    let findNodes: ISendNode[] = [];

    if (parts[0] === '*' || parts[0] === parentNode.name) {
        const innerPath = parts.slice(1).join('.');

        // Достигли конца path
        if (!innerPath) {
            // eslint-disable-next-line no-nested-ternary
            return attrs ?
                compareAttrs(attrs, parentNode.attrs) ? [parentNode] : [] :
                [parentNode];
        }

        for (let node of parentNode.children || []) {
            findNodes = findNodes.concat(query(innerPath, node, attrs));
        }
    }

    return findNodes;
}

export function findDiffNodes({
    server, client,
}: IFindDiffNodesParams): IFindDiffNodesParams | undefined {
    const childrenA = server.children || [];
    const childrenB = client.children || [];

    if (childrenA.length !== childrenB.length) return { client, server };

    for (const i in childrenB) {
        const nodeB = childrenB[i];
        const nodeA = childrenA[i];

        if (!areNodesIdentical({ server: nodeA, client: nodeB })) return { client, server };

        const resCompare = findDiffNodes({ server: nodeA, client: nodeB });

        if (resCompare) return resCompare;
    }

    return undefined;
}
