/* eslint-disable @typescript-eslint/no-var-requires,global-require */

import {IncomingHttpHeaders} from 'http';

import Layer from '../../layer';
import JestLayer from '../jest';
import {resolveScriptPath} from '../../../utils/relativePath';
import type {Config} from './worker';
import {KadavrWorker} from './worker';

export type {Config};

function init(initConfig: Config, dirname: string) {
    // eslint-disable-next-line global-require,@typescript-eslint/no-var-requires
    const stout = require('@yandex-market/stout');
    let config = stout.get('config');

    if (!config) {
        config = {};
        stout.set('config', config);
    }

    config.kadavrAvailable = true;
    config.kadavrHost = initConfig.host;
    config.kadavrPort = initConfig.port;

    const path = require('path');
    const {URL} = require('url');
    const {mockAsker} = require(path.join(dirname, './askerAdapter'));

    const {default: mockedTransport} = require(path.join(
        dirname,
        './mockedTransport',
    ));

    const {HttpTransport: BCMTransport} = require('bcm2/http/transport');
    jest.spyOn(BCMTransport.prototype, 'request').mockImplementation(function (
        this: typeof BCMTransport,
        request,
    ) {
        const {url, options} = request as {
            url: string;
            options: {
                method: string;
                headers: Record<any, any>;
                body: unknown;
            };
        };
        const targetURL = new URL(url);

        if (targetURL.host.includes(initConfig.host)) {
            return mockedTransport({
                path: targetURL.pathname + targetURL.search,
                method: options.method,
                headers: options.headers,
                body: options.body,
            });
        }

        return BCMTransport.prototype.request.call(this, request);
    });

    // eslint-disable-next-line @typescript-eslint/no-var-requires,global-require
    const RequestController = require('@yandex-market/kadavr/server/RequestController');

    jest.spyOn(RequestController.prototype, '_proxy').mockImplementation(
        function (this: typeof RequestController) {
            return Promise.reject(
                // @ts-ignore
                new Error(`[Kadavr]: Can't find mock for ${this.originalHost}`),
            );
        },
    );
    // TODO вынести API кадавра в отдельный модуль и дать возможность обрабатывать попытки пойти в реальный бэкенд
    mockAsker('asker', initConfig);
    mockAsker('@yandex-market/asker', initConfig);
}

// eslint-disable-next-line @typescript-eslint/ban-types
export default class KadavrLayer extends Layer<{}, KadavrWorker> {
    static ID = 'kadavr';

    #config: Config;

    #sessionId: string | null = null;

    constructor(config: Config) {
        super(KadavrLayer.ID, resolveScriptPath(__filename, './worker.js'));
        this.#config = config;
    }

    // eslint-disable-next-line class-methods-use-this,@typescript-eslint/ban-types
    getMethods(): {} {
        return {};
    }

    async init(): Promise<void> {
        await this.worker.init(this.#config);
        await this.getMirror()
            ?.getLayer<JestLayer>(JestLayer.ID)
            ?.backend.runCode(init, [this.#config, __dirname]);
    }

    async ensureSession(): Promise<void> {
        if (!this.#sessionId) {
            this.#sessionId = await this.worker.createSession();
        }
    }

    async getSessionId(forceRecreate = false): Promise<string | null> {
        if (forceRecreate) {
            await this.destroySession();
        }

        await this.ensureSession();
        return this.#sessionId;
    }

    async destroySession(): Promise<void> {
        this.#sessionId = null;
    }

    async setState<TData>(path: string, data: TData): Promise<void> {
        await this.ensureSession();
        await this.worker.setState(path, data);
    }

    async execMock<T>(
        url: URL,
        method: string,
        body: Buffer | null,
        headers?: IncomingHttpHeaders,
    ): Promise<T> {
        await this.ensureSession();
        return this.worker.execMock(url.toString(), method, body, headers);
    }
}
