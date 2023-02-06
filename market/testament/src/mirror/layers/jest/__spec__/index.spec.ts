/* eslint-disable no-shadow, global-require, import/no-unresolved,
no-return-assign, @typescript-eslint/no-var-requires */

import Mirror, {packFunction} from '../../..';
import JestLayer from '..';

const mirror = new Mirror();
const jestLayer = new JestLayer(__filename, jest);

beforeAll(async () => {
    await mirror.registerRuntime(jestLayer);
});

beforeEach(async () => {
    await jestLayer.runCode(() => {
        jest.resetModules();
    }, []);
});

afterAll(() => mirror.destroy());

describe('doMock', () => {
    it('should accept packed function', async () => {
        await jestLayer.doMock(
            'foo',
            packFunction((a1, a2) => a1 + a2, [1, 2]),
            {virtual: true},
        );

        const result = await jestLayer.runCode(() => require('foo'), []);

        expect(result?.getClient()).toBe(3);
        expect(result?.getBackend()).toBe(3);
    });

    describe('should mock virtual module', () => {
        test('mode: both', async () => {
            const mockFn = () => ({data: 'mocked result'});

            await jestLayer.doMock(
                'foo',
                mockFn,
                {virtual: true},
                {
                    mode: 'both',
                },
            );
            const result = await jestLayer.runCode(
                () => require('foo'),
                [],
                '',
            );
            expect(result?.getClient()).toMatchObject({data: 'mocked result'});
            expect(result?.getBackend()).toMatchObject({
                data: 'mocked result',
            });
        });

        test('mode: backend and client separately', async () => {
            const mockFn = () => ({data: 'mocked result 3'});
            const mockFn2 = () => ({data: 'mocked result 4'});

            await jestLayer.doMock(
                'foo',
                mockFn,
                {virtual: true},
                {mode: 'client'},
            );
            await jestLayer.doMock(
                'foo',
                mockFn2,
                {virtual: true},
                {mode: 'backend'},
            );
            const result = await jestLayer.runCode(
                () => require('foo'),
                [],
                '',
            );
            expect(result?.getClient()).toMatchObject({
                data: 'mocked result 3',
            });
            expect(result?.getBackend()).toMatchObject({
                data: 'mocked result 4',
            });
        });
    });
});

describe('runCode', () => {
    test('mode: both', async () => {
        const data = [1, 'hello'];
        const result = await jestLayer.runCode((foo, bar) => [foo, bar], data);

        expect(result?.getClient()).toMatchObject(data);
        expect(result?.getBackend()).toMatchObject(data);
    });

    test('mode: backend and client separately', async () => {
        // @ts-ignore
        await jestLayer.runCode(foo => (global.foo = foo), [1], '', {
            mode: 'client',
        });
        // @ts-ignore
        await jestLayer.runCode(foo => (global.foo = foo), [2], '', {
            mode: 'backend',
        });

        // @ts-ignore
        const result = await jestLayer.runCode(() => global.foo, []);

        expect(result?.getClient()).toBe(1);
        expect(result?.getBackend()).toBe(2);
    });

    describe('require inside', () => {
        test('base', async () => {
            const data = {default: 'bar'};
            const result = await jestLayer.runCode(
                () => require('./mocks/foo'),
                [],
            );

            expect(result?.getClient()).toMatchObject(data);
            expect(result?.getBackend()).toMatchObject(data);
        });

        test('extended', async () => {
            const data = 'test';
            await jestLayer.runCode(() => {
                const registry = require('./mocks/registry').default;
                registry.set('test');
            }, []);
            let result = await jestLayer.runCode(
                () => require('./mocks/bar').default(),
                [],
            );

            expect(result?.getClient()).toBe(data);
            expect(result?.getBackend()).toBe(data);

            await jestLayer.runCode(() => {
                jest.doMock('./mocks/bar');
                require('./mocks/bar').default.mockImplementation(
                    () => 'test2',
                );
            }, []);

            result = await jestLayer.runCode(
                () => require('./mocks/bar').default(),
                [],
            );

            expect(result?.getClient()).toBe('test2');
            expect(result?.getBackend()).toBe('test2');
        });

        test('getBackend', async () => {
            const result = await jestLayer.runCode(
                () => require('./mocks/getBackend').default(),
                [],
                undefined,
                {mode: 'backend'},
            );

            expect(result?.getBackend()).toBe(123);
        });
    });
});
