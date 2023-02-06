import {IncomingHttpHeaders} from 'http';

import makeResolver from '@yandex-market/kadavr/dist/mockResolver';
// @ts-ignore
// eslint-disable-next-line import/no-unresolved
import {client, options} from '@yandex-market/kadavr';
// @ts-ignore
import mocks from '@yandex-market/kadavr/options/mocks';
import State from '@yandex-market/kadavr/dist/state';
import Storage from '@yandex-market/kadavr/dist/storage/memory';
import {makeRequest, Request} from '@yandex-market/kadavr/dist/mockExecutor';
import {Methods} from '@yandex-market/kadavr/dist/mockExecutor/request';
import Mock from '@yandex-market/kadavr/dist/Mock';

import {NoMockResolverError} from './errors/noMockResolverError';
import {NoMockRouteError} from './errors/noMockRouteError';

export type Config = {
    host: string;
    port: number;
    noRandomFakes?: boolean;
};

export type CallableMock = Mock & {
    [key: string]: (request: Request<any, any>) => Promise<any>;
};

export class KadavrWorker {
    #sessionId: string | null = null;

    #resolver = makeResolver(mocks);

    #currentMocks = new Map<string, CallableMock>();

    // eslint-disable-next-line class-methods-use-this
    async init(config: Config): Promise<void> {
        options.host = config.host;
        options.port = config.port;
        options.noRandomFakes = Boolean(config.noRandomFakes);
    }

    // eslint-disable-next-line @typescript-eslint/explicit-module-boundary-types
    async setState(path: string, data: any): Promise<void> {
        await client.setState(this.#sessionId, path, data);
    }

    async getState(): Promise<any> {
        const result = await client.getState(this.#sessionId);

        return JSON.parse(result);
    }

    async execMock<T>(
        urlStr: string,
        method: string,
        body: Buffer | null,
        headers?: IncomingHttpHeaders,
    ): Promise<T> {
        if (!this.#sessionId) {
            this.#sessionId = await this.createSession();
        }

        const url = new URL(urlStr);

        const resolvedMock = this.#resolver(url.host);

        if (!resolvedMock) {
            throw new NoMockResolverError(url.host);
        }

        const resolvedRoute = resolvedMock.resolveRoute(
            url.pathname,
            method as Methods,
        );

        if (!resolvedRoute) {
            throw new NoMockRouteError(url.host, url.pathname);
        }

        const MockConstructor: new (state: State) => CallableMock =
            resolvedMock.descriptor.class;

        const state = new State(
            new Storage(await this.getState()),
            this.#sessionId,
        );

        let mock = this.#currentMocks.get(
            this.#sessionId + MockConstructor.name,
        );

        if (mock) {
            mock.state = state;
        } else {
            mock = new MockConstructor(state);

            await mock.init();
        }

        this.#currentMocks.set(this.#sessionId + MockConstructor.name, mock);

        const result = await mock[resolvedRoute.name](
            makeRequest(url, method as Methods, body?.toJSON(), headers),
        );

        return result;
    }

    async createSession(): Promise<string> {
        this.#sessionId = await client.createSession(
            `session-${(Math.random() * 100000).toFixed()}`,
        );
        return this.#sessionId as string;
    }
}

export default new KadavrWorker();
