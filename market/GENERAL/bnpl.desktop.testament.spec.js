// flowlint-next-line untyped-import: off
import {fireEvent, waitFor, screen} from '@testing-library/dom';

import {makeMirror} from '@self/platform/helpers/testament';
import {
    baseMockFunctionality,
    bnplMockFunctionality,
    mockRouter,
} from '@self/platform/spec/testament/FinancialProduct/mockFunctionality';
import * as mocks from '@self/platform/spec/testament/FinancialProduct/mockData';
import expect from 'expect';

const WIDGET_PATH = '@self/platform/widgets/content/FinancialProduct';

const {
    mockLocation,
} = require('@self/root/src/helpers/testament/mock');

/** @type {Mirror} */
let mirror;
/** @type {JestLayer} */
let jestLayer;
/** @type {MandrelLayer} */
let mandrelLayer;
/** @type {ApiaryLayer} */
let apiaryLayer;

beforeAll(async () => {
    mockLocation();
    mirror = await makeMirror({
        jest: {
            testFilename: __filename,
            jestObject: jest,
        },
        kadavr: {
            skipLayer: true,
        },
    });
    jestLayer = mirror.getLayer('jest');
    mandrelLayer = mirror.getLayer('mandrel');
    apiaryLayer = mirror.getLayer('apiary');

    const offer = {
        ...mocks.offer,
        bnplAvailable: true,
        creditInfo: null,
        installmentsInfo: null,
        financialProductPriorities: [['BNPL']],
    };
    await jestLayer.runCode(mockRouter, [mocks.page]);
    await jestLayer.backend.runCode(baseMockFunctionality, [{
        ...mocks,
        collections: {
            ...mocks.collections,
            offer: {
                [offer.id]: offer,
            },
        },
    }]);
});

afterAll(() => {
    mirror.destroy();
});

const redirectToCheckout = async term => {
    const button = screen.getByRole('to-checkout');
    fireEvent.click(button);
    await waitFor(() => {
        expect(window.location.assign).toHaveBeenCalledWith(
            `${mocks.page.routes.checkout}?bnplConstructor=${term}&bnplSelected=true`
        );
    });
};

const checkSelectedTerm = async () => {
    const plan1 = screen.getByRole('bnpl-plan-2month');
    await expect(plan1).toBeVisible();
    const switcherButton2 = screen.getByRole('bnpl-switcher-term-4month');
    fireEvent.click(switcherButton2);
    const plan2 = await screen.findByRole('bnpl-plan-4month');
    await expect(plan2).toBeVisible();
};

describe('FinancialProduct', () => {
    describe('BNPL', () => {
        beforeEach(async () => {
            await mandrelLayer.initContext({
                // не важно какая страница
                request: {params: {offerId: mocks.offer.id}},
                route: {name: mocks.page.id.offer},
            });
        });
        describe('Несколько планов', () => {
            beforeAll(async () => {
                await jestLayer.backend.runCode(bnplMockFunctionality, [mocks.bnplInfoManyPlans]);
            });

            it('Переключатель сроков отображается', async () => {
                await apiaryLayer.mountWidget(WIDGET_PATH);
                await expect(screen.getByRole('bnpl-switcher-term')).toBeInTheDocument();
            });

            it('Работает выбор нового срока', async () => {
                await apiaryLayer.mountWidget(WIDGET_PATH);
                await checkSelectedTerm();
            });

            it('Переход в чекаут после выбора другого срока', async () => {
                await apiaryLayer.mountWidget(WIDGET_PATH);
                await checkSelectedTerm();
                await redirectToCheckout('4month');
            });
        });
        describe('Один план', () => {
            beforeAll(async () => {
                await jestLayer.backend.runCode(bnplMockFunctionality, [mocks.bnplInfoOnePlan]);
            });

            it('Переключатель сроков не отображается', async () => {
                await apiaryLayer.mountWidget(WIDGET_PATH);
                await expect(screen.queryByRole('bnpl-switcher-term')).not.toBeInTheDocument();
            });

            it('Переход в чекаут', async () => {
                await apiaryLayer.mountWidget(WIDGET_PATH);
                await redirectToCheckout('2month');
            });
        });
    });
});
