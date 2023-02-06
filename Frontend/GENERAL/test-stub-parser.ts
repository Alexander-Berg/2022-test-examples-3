import _ from 'lodash';
import { NodeVM } from 'vm2';
import DeepProxy from 'proxy-deep';

import { HermioneStubParserConfig, TestFileData } from '../../types';
import {
    TestFunction,
    HookFunction,
    QueueTask,
    TestStubParserContext,
    HermioneGlobal,
    SuiteCallback,
    TestCallback,
} from './types';
import { defaultOptions } from './parser';

export default class TestStubParser {
    private _options: HermioneStubParserConfig;
    private _originalGlobal: Record<string, any>;
    private _parseStubs: Set<any>;

    constructor(options: Partial<HermioneStubParserConfig>) {
        this._options = _.defaultsDeep(options, defaultOptions);
        this._originalGlobal = {};
        this._parseStubs = new Set(this._options.parseStubs);
    }

    async parse(raw: string, filePath: string): Promise<TestFileData> {
        const thisParser = this;
        const context: TestStubParserContext = {
            filePath,
            data: {
                specs: {},
                files: [filePath],
            },
        };
        const queue: QueueTask[] = [];
        const errors: Error[] = [];
        const vm = new NodeVM({
            sandbox: this._createContext(context, queue),
            require: {
                builtin: ['*'],
                context: 'sandbox',
                external: true,
                mock: new Proxy(
                    {},
                    {
                        get(_target, prop): any {
                            const targetPattern = _.keys(thisParser._options.requireStubs).find(
                                (pattern: RegExp | string): boolean => {
                                    return new RegExp(pattern).test(prop as string);
                                },
                            );

                            if (!targetPattern) {
                                return;
                            }

                            // Меняем название объекта на PO, чтобы не писать туда путь
                            if (
                                /[\.\/]page-object/.test(prop as string) ||
                                /\.po/.test(prop as string)
                            ) {
                                prop = 'PO';
                            }

                            return thisParser._proxyStub(
                                _.get(thisParser._options, ['requireStubs', targetPattern], {}),
                                context,
                                prop as string,
                            );
                        },
                    },
                ),
            },
        });

        try {
            vm.run(raw, filePath);

            // здесь важно использовать цикл, потому что очередь обновляется в процессе итераций
            while (queue.length) {
                try {
                    const task = queue.shift();

                    if (task) {
                        context.titlePath = task.titlePath;

                        await task.cb.call(this._proxyStub(this._options.thisStubs, context));
                    }
                } catch (error) {
                    // Ошибки складываем в очередь, т.к. на этом этапе глобальное окружение остается пропатченным,
                    // но console.error нужен нормальный объект JSON
                    errors.push(error);
                }
            }
        } catch (error) {
            errors.push(error);
        }

        for (const error of errors) {
            if (!this._options.silent) {
                console.log(`ParsingError in ${filePath}`);

                if (this._options.verbose) {
                    console.error(error);
                } else {
                    console.error(error.message);
                }
            }
        }

        return context.data;
    }

    private _createContext(context: TestStubParserContext, queue: QueueTask[]): HermioneGlobal {
        const globalContext: Record<string, any> = {};
        const describeStub = this._describeStub(context, queue);
        const beforeEachStub = this._beforeEachStub(context, queue);
        const afterEachStub = this._afterEachStub(context, queue);
        const itStub = this._itStub(context, queue);

        ['specs', 'describe', 'beforeEach', 'afterEach', 'it', 'h'].forEach((object) => {
            if (!this._originalGlobal[object]) {
                this._originalGlobal[object] = globalContext[object];
            }
        });

        globalContext.hermione = this._proxyStub({}, context, 'hermione');
        globalContext.specs = describeStub;
        globalContext.describe = describeStub;
        globalContext.beforeEach = beforeEachStub;
        globalContext.afterEach = afterEachStub;
        globalContext.it = itStub;

        globalContext.h = {};
        globalContext.h.describe = describeStub;
        globalContext.h.beforeEach = beforeEachStub;
        globalContext.h.afterEach = afterEachStub;
        globalContext.h.it = itStub;

        _.keys(this._options.globalStubs).forEach((object) => {
            if (!this._originalGlobal[object]) {
                this._originalGlobal[object] = globalContext[object];
            }

            globalContext[object] = this._proxyStub(
                _.get(this._options, ['globalStubs', object], {}),
                context,
                object,
            );
        });

        return globalContext;
    }

    private _describeStub(
        context: TestStubParserContext,
        queue: QueueTask[],
    ): (name: string | Record<string, string>, cb: SuiteCallback) => Promise<void> {
        return async function (
            name: string | Record<string, string>,
            cb: SuiteCallback,
        ): Promise<void> {
            const prevTitlePath = context.titlePath;
            if (typeof name === 'string') {
                if (!context.titlePath) {
                    context.titlePath = [];
                    _.set(context, 'data.feature', name);
                } else {
                    context.titlePath = [...context.titlePath, name];
                }
            } else {
                context.titlePath = context.titlePath || [];
                _.defaultsDeep(context.data, name);
            }

            _.defaultsDeep(context.data.specs, _.set({}, context.titlePath, {}));

            queue.push({
                titlePath: context.titlePath,
                cb,
            });

            if (prevTitlePath) {
                context.titlePath = prevTitlePath;
            } else {
                context.titlePath = [];
            }
        };
    }

    private _itStub(context: TestStubParserContext, queue: QueueTask[]): TestFunction {
        return this._runnableStub(context, queue);
    }

    private _beforeEachStub(context: TestStubParserContext, queue: QueueTask[]): HookFunction {
        return this._runnableStub(context, queue, 'beforeEach');
    }

    private _afterEachStub(context: TestStubParserContext, queue: QueueTask[]): HookFunction {
        return this._runnableStub(context, queue, 'afterEach');
    }

    private _runnableStub(
        context: TestStubParserContext,
        queue: QueueTask[],
        name?: string,
    ): TestFunction & HookFunction {
        return async function (nameOrCb: string | TestCallback, cb?: TestCallback): Promise<void> {
            const prevTitlePath = context.titlePath;

            if (typeof nameOrCb === 'string' && cb) {
                name = nameOrCb;
            } else {
                cb = nameOrCb as TestCallback;
            }

            context.titlePath = context.titlePath ? [...context.titlePath, name] : [name];

            _.defaultsDeep(context.data.specs, _.set({}, context.titlePath, []));

            queue.push({
                titlePath: context.titlePath,
                cb,
            });

            if (prevTitlePath) {
                context.titlePath = prevTitlePath;
            } else {
                context.titlePath = [];
            }
        };
    }

    private _proxyStub(
        targetObject: Record<string, any> = {},
        context: TestStubParserContext,
        objectName?: string,
    ): Record<string, any> {
        const thisParser = this;
        const proxyTarget = _.merge(new Function(), targetObject);

        return new DeepProxy(proxyTarget, {
            set(): any {
                return true;
            },
            get(target, prop, receiver): any {
                const desc = Object.getOwnPropertyDescriptor(target, prop);
                const value = Reflect.get(target, prop, receiver);
                const self = this;

                if (this.path.length > 100) {
                    return;
                }

                if (prop === Symbol.iterator) {
                    return function (): Record<string, any> {
                        return {
                            _current: 1,
                            next(): any {
                                const result = this._current
                                    ? { done: false, value: self.nest() }
                                    : { done: true };

                                this._current--;

                                return result;
                            },
                        };
                    };
                }

                // TODO: подумать, как лучше здесь решить
                if (['length', 'size'].includes(prop as string) && value === undefined) {
                    return 1;
                }

                // Если запрашивается служебное свойство, то отдаём значение
                // TODO: разобраться с then — сейчас не работает обработка чейнинга
                if (
                    (desc && !desc.writable && !desc.configurable) ||
                    ['call', 'apply', 'bind', 'then', 'toString', 'toJSON', 'valueOf'].includes(
                        prop as string,
                    ) ||
                    String(prop).startsWith('_') ||
                    typeof prop === 'symbol'
                ) {
                    return value;
                }

                const proxyPath = this.path.filter((part) => typeof part === 'string');

                // Отдаём proxy функции-объекта, так как мы не знаем что там и будет ли это вызываться
                return thisParser._proxyValue(this, value, proxyPath.join('.'));
            },
            apply(target, _thisArg, argList): any {
                const name = this.path[this.path.length - 1];
                const value = Reflect.apply(target as Function, { context }, argList);
                const object = objectName || this.path[0];
                const proxyPath = (
                    objectName ? [String(objectName), ...this.path] : this.path
                ).filter((part) => typeof part === 'string');
                const write = thisParser._parseStubs.has(object);

                if (write) {
                    argList = argList.map((arg: any) => {
                        if (_.isObject(arg)) {
                            try {
                                return JSON.parse(JSON.stringify(arg));
                            } catch (_error) {}
                        }

                        return arg;
                    });

                    const step = {
                        name,
                        arguments: argList,
                        object,
                        path: proxyPath,
                    };

                    if (_.get(context, 'titlePath.length')) {
                        _.get(context.data.specs, context.titlePath).push(step);
                    }
                }

                // TODO: подумать, как при apply отдавать новый proxy объект без записи steps
                return thisParser._proxyValue(this, value, `${proxyPath.join('.')}()`);
            },
        });
    }

    _proxyValue(proxy: Record<string, any>, value: any, proxyPath: string): any {
        if (!value) {
            const raw = Object.defineProperties(new Function(), {
                [Symbol.for('raw')]: {
                    value: proxyPath,
                    enumerable: false,
                },
                toString: {
                    value: function (): string {
                        return this[Symbol.for('raw')];
                    },
                    enumerable: false,
                },
            });

            return proxy.nest(raw);
        }

        if (_.isArray(value)) {
            return value.map((item, i) => this._proxyValue(proxy, item, `${proxyPath}.${i}`));
        }

        if (
            (_.isFunction(value) || typeof value === 'object') &&
            !_.isRegExp(value) &&
            !_.isNull(value)
        ) {
            if (value.name === 'call' || value.name === 'toString' || value.name === 'valueOf') {
                return value;
            }

            return proxy.nest(value);
        }

        return value;
    }
}
