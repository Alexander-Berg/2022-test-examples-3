/* eslint-disable no-shadow, global-require, import/no-unresolved, no-return-assign */

import Mirror from '../../..';
import NativeLayer from '..';
import {resolveScriptPath} from '../../../../utils/relativePath';

const mirror = new Mirror();
const nativeRuntime = new NativeLayer(__filename);

beforeAll(async () => {
    await mirror.registerRuntime(nativeRuntime);
});

afterAll(() => mirror.destroy());

describe('runCode', () => {
    test('mode: both', async () => {
        const data = [1, 'hello'];
        const result = await nativeRuntime.runCode(
            (foo, bar) => [foo, bar],
            data,
        );

        expect(result?.getClient()).toMatchObject(data);
        expect(result?.getBackend()).toMatchObject(data);
    });

    test('mode: backend and client separately', async () => {
        // @ts-ignore
        await nativeRuntime.runCode(foo => (global.foo = foo), [1], '', {
            mode: 'client',
        });
        // @ts-ignore
        await nativeRuntime.runCode(foo => (global.foo = foo), [2], '', {
            mode: 'backend',
        });

        // @ts-ignore
        const result = await nativeRuntime.runCode(() => global.foo, []);

        expect(result?.getClient()).toBe(1);
        expect(result?.getBackend()).toBe(2);
    });
});

describe('require', () => {
    test('mode: both', async () => {
        const result = await nativeRuntime.requireModule(
            resolveScriptPath(__filename, './mock.js'),
        );

        expect(result?.getClient()).toMatchObject({default: 'ok'});
        expect(result?.getBackend()).toMatchObject({default: 'ok'});
    });
});
