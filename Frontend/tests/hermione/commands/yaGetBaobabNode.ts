import _ from 'lodash';
import type { INodeAttrs } from '@yandex-int/react-baobab-logger/lib/common/Baobab/Baobab.typings/common';
import type { ISendNode } from '@yandex-int/react-baobab-logger/lib/common/Sender/Sender.typings/Sender';
import { query } from '../utils/baobab';

interface INodeParams {
    path: string;
    attrs?: INodeAttrs;
    index?: number;
    last?: boolean;
    /**
     * Параметр говорит где искать ноду
     * redir - в текущем дереве на клиенте
     * blockstat - в дереве, отправленном с сервера. В этом дереве не будет изменений,
     * прозошедших в результате действий в браузере
     */
    source?: 'blockstat' | 'redir';
}

// eslint-disable-next-line valid-jsdoc
/** Поиск ноды в баобаб-дереве с путем path и атрибутами attrs */
export async function yaGetBaobabNode(
    this: WebdriverIO.Browser,
    { path, attrs, index, last, source }: INodeParams,
): Promise<ISendNode | undefined> {
    if (source) {
        const tree = source === 'redir' ?
            await this.execute(function() {
                // @ts-expect-error
                return (window.Ya.Baobab.logger.getRootSendNode()) as ISendNode;
            }) :
            (await this.yaGetBaobabTree()).tree;

        const nodes = query(path, tree, attrs);
        return last ? _.last(nodes) : nodes[index || 0];
    }

    // если source не передан, то сначала ищем в blockstat
    let node = await this.yaGetBaobabNode({
        path, attrs, index, last, source: 'blockstat',
    });

    // если в blockstat нет такой ноды, то ищем в redir
    if (!node) {
        node = await this.yaGetBaobabNode({
            path, attrs, index, last, source: 'redir',
        });
    }

    return node;
}
