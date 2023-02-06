/**
 * Copy-paste аналогичной команды из web4
 *
 * @see https://github.yandex-team.ru/serp/web4/blob/dev/hermione/commands/commands-templar/common/yaCheckBaobabCounter.js
 */
import _ from 'lodash';
import type { INodeAttrs } from '@yandex-int/react-baobab-logger/lib/common/Baobab/Baobab.typings/common';
import type { ISendNode } from '@yandex-int/react-baobab-logger/lib/common/Sender/Sender.typings/Sender';
import type { IEventData } from '@yandex-int/react-baobab-logger/lib/common/Baobab/Baobab.typings/Baobab_dynamic';

import { query } from '../utils/baobab';
import type { IClientCounter } from '../../typings';

interface IExpected {
    /** Путь до ноды в дереве */
    path: string;
    /** Атрибуты до ноды */
    attrs?: INodeAttrs;
    /** Дополнительные данные, отправленные в событии */
    data?: Record<string, unknown>;
}

interface IGroup {
    nodes: ISendNode[];
    expected: IExpected;
}

interface IOptions {
    blockstatReqid?: string;
    redirReqid?: string;
    message?: string;
}

const validator = (counters: IClientCounter, groups: IGroup[], options: IOptions) => {
    const client = counters.client;
    for (let group of groups) {
        const nodeCounters: IEventData = [];
        const nodes = group.nodes;
        const expected = group.expected;

        let found = client.find(item => {
            const events = item.events;

            if (events) {
                let data: IEventData = JSON.parse(decodeURIComponent(events))[0];

                return nodes.find(node => {
                    if (node.id !== data.id) {
                        return false;
                    }

                    // Не проверяем динамическую информацию, уникальную для каждого счетчика
                    data = _.omit(data, ['cts', 'mc', 'hdtime', 'event-id']);
                    data.data = _.omit(data.data, ['legacy-reqid']);

                    if (_.isEmpty(data.data)) {
                        delete data.data;
                    }

                    nodeCounters.push(data);

                    return _.isMatch(
                        data,
                        Object.assign({
                            id: node.id,
                            event: 'click',
                        }, _.omit(expected, ['path', 'attrs'])),
                    );
                });
            } else if (item.bu) {
                return nodes.some(node => node.id === item.bu);
            }

            return false;
        });

        if (!found) {
            let message = options.message ? `${options.message}\n` : '';
            message += `В логах не найдена запись для: ${JSON.stringify(expected, null, 2)}`;

            if (nodeCounters.length) {
                message += `\n\nВсе сработавшие счетчики на узле c ${expected.path}: ${JSON.stringify(nodeCounters, null, 2)}`;
            }

            throw new Error(message);
        }
    }
};

/**
 * Проверить отправку события баобаб дерева.
 * Поиск ноды в баобаб-дереве с путем expected.path и атрибутами expected.attrs
 */
export function yaCheckBaobabEvent(
    this: WebdriverIO.Browser,
    expected: IExpected | IExpected[],
    options: IOptions = {},
) {
    const exprectedArr = Array.isArray(expected) ? expected : [expected];

    return Promise.resolve()
        .then(() => this.yaGetBaobabTree(options.blockstatReqid))
        .then(tree => {
            const groups = exprectedArr.map(expectedItem => {
                // Находим узел дерева по path и attrs, их может быть несколько
                const nodes = query(expectedItem.path, tree.tree, expectedItem.attrs);

                if (!nodes.length) {
                    let message = options.message ? `${options.message}\n` : '';
                    message += `${options.message}\nВ дереве Баобаба не найден узел с путем ${expectedItem.path}`;

                    if (expectedItem.attrs) {
                        message += ` и атрибутами ${JSON.stringify(expectedItem.attrs)}`;
                    }

                    throw new Error(message);
                }

                return { nodes, expected: expectedItem };
            });

            return this.yaGetCounters(
                counters => validator(counters as IClientCounter, groups, options),
                options.redirReqid,
            );
        });
}
