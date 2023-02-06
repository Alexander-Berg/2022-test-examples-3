import type { IServerCounter, ICounter } from '../../typings';

const validator = (counters: ICounter) => {
    assert.isDefined(
        (counters as IServerCounter)?.server?.tree,
        'Не удалось получить баобаб-дерево из серверных счётчиков',
    );
};

/** Получить из blockstat-лога дерево баобаба */
export function yaGetBaobabTree(this: WebdriverIO.Browser, reqid?: string) {
    return Promise.resolve()
        .then(() => this.yaGetCounters(validator, reqid))
        .then(counters => (counters as IServerCounter).server.tree);
}
