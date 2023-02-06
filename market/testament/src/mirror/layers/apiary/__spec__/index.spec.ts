/* eslint-disable no-shadow, global-require, import/no-unresolved, no-return-assign,
@typescript-eslint/no-var-requires */

import * as path from 'path';

import {waitFor} from 'playwright-testing-library';
import {getByRole, queryAllByRole, queryByRole} from '@testing-library/dom';

import Mirror from '../../..';
import JestLayer from '../../jest';
import MandrelLayer from '../../mandrel';
import ApiaryLayer from '..';
import PackedFunction from '../../../packedFunction';

const mirror = new Mirror();
const jestLayer = new JestLayer(__filename, jest);
const mandrelLayer = new MandrelLayer();
const apiaryLayer = new ApiaryLayer();

const bootstrapLayers = async () => {
    await mandrelLayer.initContext();
};

beforeAll(async () => {
    await mirror.registerRuntime(jestLayer);
    await mirror.registerLayer(mandrelLayer);
    await mirror.registerLayer(apiaryLayer);
});

beforeEach(async () => {
    await bootstrapLayers();
});

afterAll(() => mirror.destroy());

describe('mountWidget', () => {
    test('should work', async () => {
        const {container, data} = await apiaryLayer.mountWidget(
            '../../../../platform/apiary/__tests__/MyWidget',
            {
                items: [1, 2],
            },
        );
        expect(queryAllByRole(container, 'item')).toHaveLength(2);
        getByRole(container, 'my-button').click();
        expect(queryAllByRole(container, 'item')).toHaveLength(0);
        expect(queryByRole(container, 'is-robot')?.textContent).toBe(
            'not robot',
        );
        expect(data).toMatchSnapshot();
    });

    test('should expose runtime', async () => {
        const {container, runtime} = await apiaryLayer.mountWidget(
            '../../../../platform/apiary/__tests__/MyWidget',
            {
                items: [1, 2],
            },
        );
        expect(queryAllByRole(container, 'item')).toHaveLength(2);
        runtime
            ._selectStoreByWidgetSource(null)
            .dispatch({type: '#ONE', meta: {widgetId: '/'}});
        expect(queryAllByRole(container, 'item')).toHaveLength(0);
    });

    test('isRobot', async () => {
        await mandrelLayer.initContext({
            user: {
                region: {
                    id: 12345,
                },
                isRobot: true,
            },
        });

        const {container, data} = await apiaryLayer.mountWidget(
            '../../../../platform/apiary/__tests__/MyWidget',
            {
                items: [1, 2],
            },
        );
        expect(queryAllByRole(container, 'item')).toHaveLength(2);
        getByRole(container, 'my-button').click();
        expect(queryAllByRole(container, 'item')).toHaveLength(2);
        expect(queryByRole(container, 'is-robot')?.textContent).toBe('robot');
        expect(data).toMatchSnapshot();
    });

    test('should throw valid error on failed widget mount (err in widget)', async () => {
        const pathToWidget =
            '../../../../platform/apiary/__tests__/ErrorWidget';

        const tryMountWidget = () =>
            apiaryLayer.mountWidget(pathToWidget, {
                items: [1, 2],
            });

        const resolvedPathToWidget = path.resolve(
            __dirname,
            pathToWidget,
            'index.ts',
        );

        await expect(tryMountWidget()).rejects.toThrow(new Error('My test'));
        // TODO MARKETFRONTECH-4364 сейчас текст ошибки будет другой
        // Проверяем что на втором запуске правильно инвалидируются кеши модулей и бросеатся нужная ошибка
        await expect(tryMountWidget()).rejects.toThrow(
            new Error(
                `Widget ${resolvedPathToWidget} is empty or has errors. See log above.`,
            ),
        );
    });

    test('should throw valid error on failed widget mount (empty file)', async () => {
        const pathToWidget =
            '../../../../platform/apiary/__tests__/EmptyWidget';

        const tryMountWidget = async () => {
            await apiaryLayer.mountWidget(pathToWidget, {
                items: [1, 2],
            });
        };

        const resolvedPathToWidget = path.resolve(
            __dirname,
            pathToWidget,
            'index.ts',
        );

        await expect(tryMountWidget()).rejects.toThrow(
            new Error(
                `Widget ${resolvedPathToWidget} is empty or has errors. See log above.`,
            ),
        );
    });

    describe('should clear apiary-runtime', () => {
        beforeEach(async () => {
            await bootstrapLayers();
        });

        test('one', async () => {
            await mandrelLayer.initContext();
            const {container} = await apiaryLayer.mountWidget(
                '../../../../platform/apiary/__tests__/MyWidget',
                {
                    items: [1, 2],
                },
            );
            expect(getByRole(container, 'touches').textContent).toBe('0');
            getByRole(container, 'touch-button').click();
            expect(getByRole(container, 'touches').textContent).toBe('1');
        });

        test('two', async () => {
            await mandrelLayer.initContext();
            const {container} = await apiaryLayer.mountWidget(
                '../../../../platform/apiary/__tests__/MyWidget2',
                {
                    items: [1, 2],
                },
            );
            expect(getByRole(container, 'touches').textContent).toBe('0');
            getByRole(container, 'touch-button').click();
            expect(getByRole(container, 'touches').textContent).toBe('1');
        });

        test('three', async () => {
            await mandrelLayer.initContext();
            const {container} = await apiaryLayer.mountWidget(
                '../../../../platform/apiary/__tests__/MyWidget',
                {
                    items: [1, 2],
                },
            );
            expect(getByRole(container, 'touches').textContent).toBe('0');
            getByRole(container, 'touch-button').click();
            expect(getByRole(container, 'touches').textContent).toBe('1');
        });
    });
});

describe('function as props', () => {
    beforeEach(async () => {
        await mandrelLayer.initContext();
    });

    beforeAll(async () => {
        await jestLayer.doMock(
            './props.json',
            () => ({
                items: [1, 2],
            }),
            {virtual: true},
            {mode: 'backend'},
        );
    });

    test('return literal', async () => {
        const {container, data} = await apiaryLayer.mountWidget(
            '../../../../platform/apiary/__tests__/MyWidget',
            () => require('./props.json'),
        );
        expect(queryAllByRole(container, 'item')).toHaveLength(2);
        getByRole(container, 'my-button').click();
        expect(queryAllByRole(container, 'item')).toHaveLength(0);
        expect(data).toMatchSnapshot();
    });

    test('return promise', async () => {
        const {container, data} = await apiaryLayer.mountWidget(
            '../../../../platform/apiary/__tests__/MyWidget',
            () => Promise.resolve(require('./props.json')),
        );
        expect(queryAllByRole(container, 'item')).toHaveLength(2);
        getByRole(container, 'my-button').click();
        expect(queryAllByRole(container, 'item')).toHaveLength(0);
        expect(data).toMatchSnapshot();
    });

    test('packed function', async () => {
        const {container, data} = await apiaryLayer.mountWidget(
            '../../../../platform/apiary/__tests__/MyWidget',
            new PackedFunction(
                propsPath => require(propsPath),
                ['./props.json'],
            ),
        );
        expect(queryAllByRole(container, 'item')).toHaveLength(2);
        getByRole(container, 'my-button').click();
        expect(queryAllByRole(container, 'item')).toHaveLength(0);
        expect(data).toMatchSnapshot();
    });
});

it('should not import controller', async () => {
    const result = await jestLayer.runCode(
        () =>
            require('../../../../platform/apiary/__tests__/MyWidget/controller')
                .default == null,
        [],
    );
    expect(result.getClient()).toBe(true);
    expect(result.getBackend()).toBe(false);
});

it('should apply remote-resolver transformer', async () => {
    const result = await jestLayer.runCode(
        async () => ({
            function: require('./resolvers')
                .default.toString()
                .replaceAll(process.cwd(), '<rootDir>'),
            result: await require('./resolvers').default({}, {foo: 1}),
        }),
        [],
    );
    expect(result.getClient()).toMatchSnapshot('client');
    expect(result.getBackend()).toMatchSnapshot('backend');
});

describe('logging store events', () => {
    beforeEach(async () => {
        await mandrelLayer.initContext();
    });

    beforeAll(async () => {
        await jestLayer.doMock(
            './props.json',
            () => ({
                items: [1, 2],
            }),
            {virtual: true},
            {mode: 'backend'},
        );
    });

    test('dispatch event on click', async () => {
        const {container, data, runtime} = await apiaryLayer.mountWidget(
            '../../../../platform/apiary/__tests__/MyWidget',
            new PackedFunction(
                propsPath => require(propsPath),
                ['./props.json'],
            ),
        );
        getByRole(container, 'my-button').click();
        const store = runtime._selectStoreByWidgetSource(null);
        await waitFor(() => {
            expect(store.dispatch).toHaveBeenCalledWith(
                expect.objectContaining({type: '#ONE'}),
            );
        });
    });
});
