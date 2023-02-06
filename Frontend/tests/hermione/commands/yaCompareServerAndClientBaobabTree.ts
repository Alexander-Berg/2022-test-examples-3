import type { ISendNode } from '@yandex-int/react-baobab-logger/lib/common/Sender/Sender.typings/Sender';
import { findDiffNodes } from '../utils/baobab';

/** Сравнивает баобаб дерево, построенное на сервере, с клиентским баобаб деревом */
export async function yaCompareServerAndClientBaobabTree(this: WebdriverIO.Browser) {
    const serverTree = await this.yaGetBaobabTree();

    const clientTree = await this.execute(function() {
        // @ts-expect-error
        const tree: ISendNode = window.Ya.Baobab.logger.getRootSendNode();
        return tree;
    });

    if (!serverTree) {
        throw new Error('Не удалось получить серверное дерево из blockstat лога');
    }

    if (!clientTree) {
        throw new Error('Не удалось получить клиентское дерево из window.Ya.Baobab');
    }

    const diffNode = findDiffNodes({ server: serverTree.tree, client: clientTree });
    assert.notExists(diffNode, 'Есть отличие между клиентским и серверным деревом');

    return this;
}
