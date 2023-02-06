// flowlint-next-line untyped-import: off
import {fireEvent, waitFor, screen} from '@testing-library/dom';
import expect from 'expect';

import {makeMirror} from '@self/platform/helpers/testament';
import * as actions from '@self/platform/actions/checkout/fromOffer';

import {backendMock, bnplInfoManyPlans, bnplInfoOnePlan} from './mock';

const WIDGET_PATH = '@self/platform/widgets/content/offer/FinancialProduct';

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
});

afterAll(() => {
    mirror.destroy();
});

const {PAYMENTS} = actions;

const params = {
    offerId: '123',
    price: {value: 3000},
    regionId: '213',
    creditInfo: null,
    installmentInfo: null,
    yandexBnplInfo: {enabled: true},
};
const toCheckoutSpy = jest.spyOn(actions, 'toCheckout');
const redirectToCheckout = async term => {
    const button = screen.getByRole('to-checkout');
    fireEvent.click(button);
    await waitFor(() => {
        expect(toCheckoutSpy).toHaveBeenCalledWith({
            offerId: params.offerId,
            regionId: params.regionId,
            payment: PAYMENTS.YANDEX,
            params: {
                bnplSelected: true,
                bnplConstructor: term,
                installmentTerm: undefined,
            },
        });
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
            await mandrelLayer.initContext();
        });
        describe('Несколько планов', () => {
            beforeAll(async () => {
                await jestLayer.backend.runCode(backendMock, [bnplInfoManyPlans, {BNPL: true}]);
            });

            it('Переключатель сроков отображается', async () => {
                await apiaryLayer.mountWidget(WIDGET_PATH, params);
                await expect(screen.getByRole('bnpl-switcher-term')).toBeInTheDocument();
            });

            it('Работает выбор нового срока', async () => {
                await apiaryLayer.mountWidget(WIDGET_PATH, params);
                await checkSelectedTerm();
            });

            it('Вызывает событие редиректа с нужными парамтрами', async () => {
                await apiaryLayer.mountWidget(WIDGET_PATH, params);
                await checkSelectedTerm();
                await redirectToCheckout('4month');
            });
        });
        describe('Один план', () => {
            beforeAll(async () => {
                await jestLayer.backend.runCode(backendMock, [bnplInfoOnePlan, {BNPL: true}]);
            });

            it('Переключатель сроков не отображается', async () => {
                await apiaryLayer.mountWidget(WIDGET_PATH, params);
                await expect(screen.queryByRole('bnpl-switcher-term')).not.toBeInTheDocument();
            });

            it('Вызывает событие редиректа с нужными парамтрами', async () => {
                await apiaryLayer.mountWidget(WIDGET_PATH, params);
                await redirectToCheckout('2month');
            });
        });
    });
});
