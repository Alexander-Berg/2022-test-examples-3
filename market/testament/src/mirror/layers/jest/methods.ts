import Method from '../../method';
import PackedFunction from '../../packedFunction';

import type {AnyFn} from '.';
import type JestLayer from '.';

import MockOptions = jest.MockOptions;

export type Methods = {
    doMock: Method<
        [
            module: string,
            factory: AnyFn | PackedFunction<any, any>,
            options: MockOptions,
        ],
        void
    >;
    requireModule: Method<[path: string], any>;
    runCode: Method<[fn: AnyFn, args: readonly any[]], any>;
    // todo spyOn
};

export default function makeMethods(layer: JestLayer): Methods {
    return {
        doMock: new Method(
            'doMock',
            async (
                moduleName: string,
                moduleFactory: AnyFn | PackedFunction<any, any>,
                options: MockOptions,
            ) => {
                if (moduleFactory instanceof PackedFunction) {
                    jest.doMock(
                        moduleName,
                        () =>
                            layer.callFunction(
                                moduleFactory.getFn(),
                                moduleFactory.getArgs(),
                            ),
                        options,
                    );
                    return;
                }

                jest.doMock(
                    moduleName,
                    () => layer.callFunction(moduleFactory, []),
                    options,
                );
            },
            async (moduleName, moduleFactory, options) =>
                layer.worker.doMock(
                    moduleName,
                    moduleFactory.toString(),
                    options,
                ),
        ),

        requireModule: new Method(
            'requireModule',
            async (moduleName: string) => require(moduleName), // eslint-disable-line
            moduleName => layer.worker.requireModule(moduleName),
        ),

        runCode: new Method(
            'runCode',
            // todo packed function
            (code: AnyFn, args: readonly any[]) =>
                layer.callFunction(code, args as any[]),
            (code, args) => layer.worker.runCode(code.toString(), args),
        ),
    };
}
