interface ISpyXMLHttpRequest extends XMLHttpRequest {
    spy: {
        method: string;
        url: string;
        body: Document | BodyInit | null;
    };
}

type TPredicate = (this: void, value: ISpyXMLHttpRequest) => boolean;

interface IOptions {
    interval?: number;
    timeout?: number;
    timeoutMsg?: string;
}

/**
 * Возвращает первый запрос, удовлетворяющий условию.
 *
 * Запросы проксируются в клиентском скрипте.
 * @see .config/kotik/testing/hermione-assets/spyXHR.js
 */
export async function yaFindXHR(
    this: WebdriverIO.Browser,
    predicate: TPredicate,
    options: IOptions = {},
): Promise<ISpyXMLHttpRequest> {
    const {
        interval,
        timeout,
        timeoutMsg = 'Не был найден подходящий XHR',
    } = options;
    let found: ISpyXMLHttpRequest | undefined;

    await this.yaWaitUntil(timeoutMsg, async() => {
        const spies = await this.execute<ISpyXMLHttpRequest[], []>(() => {
            // @ts-expect-error у нас в тайпингах не объявлено поле гермионы
            return window.hermione.xhrSpies;
        });
        const xhr = spies.find(predicate);
        if (xhr) {
            found = xhr;
            return true;
        }

        return false;
    }, timeout, interval);

    return found as ISpyXMLHttpRequest;
}
