import {Form} from 'react-final-form';
import noop from 'lodash/noop';
import {createStore, Store} from 'redux';
import {Provider} from 'react-redux';

import {IBookingVariantModel} from 'server/services/AviaBookingService/variants/types';

import {DEFAULT_CURRENCIES} from 'reducers/common/currencies/reducer';

import {
    render,
    fireEvent,
    waitForElement,
    cleanup,
} from '@testing-library/react';

import variants from './mocks/variants.json';
import TariffsField from '../TariffsField';

const variantToken = variants[0].id;

interface ITestProps {
    initialValues?: object;
    reduxStore?: Store;
    onSubmit?: (data: object) => any;
    disabled?: boolean;
}

const commonReduxState = {
    common: {
        currencies: {
            data: DEFAULT_CURRENCIES,
        },
        experiments: {},
    },
};

const reduxStoreWithClosedModal = createStore(() => ({
    ...commonReduxState,
    avia: {
        aviaBooking: {
            tariffsModalIsOpened: false,
        },
    },
}));

const reduxStoreWithOpenedModal = createStore(() => ({
    ...commonReduxState,
    avia: {
        aviaBooking: {
            tariffsModalIsOpened: true,
        },
    },
}));

function renderWithFormContext({
    initialValues = {flightInfo: {variantToken: variantToken.toString()}},
    onSubmit = noop,
    reduxStore = reduxStoreWithClosedModal,
    disabled,
}: ITestProps) {
    const element = (
        <Provider store={reduxStore}>
            <Form initialValues={initialValues} onSubmit={onSubmit}>
                {({handleSubmit}) => (
                    <form onSubmit={handleSubmit}>
                        <TariffsField
                            variants={variants as any[]}
                            disabled={disabled}
                        />
                    </form>
                )}
            </Form>
        </Provider>
    );

    return render(element);
}

function getVariantTariffTitle(variant: IBookingVariantModel) {
    return variant.segments[0].flights[0].fareTerms.tariffGroupName;
}

describe('<TariffsField />', () => {
    afterEach(cleanup);

    test('renders first two options', () => {
        const {getByText} = renderWithFormContext({});

        variants.slice(0, 2).forEach(variant => {
            const firstSegmentTariffTitle = getVariantTariffTitle(
                variant as any,
            );

            expect(
                getByText(firstSegmentTariffTitle, {exact: false}),
            ).toBeTruthy();
        });
    });

    test('first option is checked', () => {
        const {getByLabelText} = renderWithFormContext({});
        const firstSegmentTariffTitle = getVariantTariffTitle(
            variants[0] as any,
        );
        const input = getByLabelText(firstSegmentTariffTitle, {
            exact: false,
        }) as HTMLInputElement;

        expect(input.checked).toBeTruthy();
    });

    test('submits with checked value', () => {
        const onSubmit = jest.fn();
        const {getByLabelText, container} = renderWithFormContext({onSubmit});
        const secondVariantTariffTitle = getVariantTariffTitle(
            variants[1] as any,
        );
        const input = getByLabelText(secondVariantTariffTitle, {exact: false});

        fireEvent.click(input);

        const form = container.querySelector('form')!;

        fireEvent.submit(form);

        expect(onSubmit).toHaveBeenCalledTimes(1);
        expect(onSubmit.mock.calls[0][0].flightInfo).toEqual({
            variantToken: variants[1].id,
        });
    });

    test('opens modal with all tariffs', async () => {
        const {container, getByText} = renderWithFormContext({
            reduxStore: reduxStoreWithOpenedModal,
        });
        const openButton = getByText('Показать все тарифы');

        fireEvent.click(openButton);

        const elements = await waitForElement(
            () =>
                variants.map(variant => {
                    return getByText(getVariantTariffTitle(variant as any), {
                        exact: false,
                        selector: '.below_2',
                    });
                }),
            {container},
        );

        elements.forEach(element => {
            expect(element).toBeTruthy();
        });
    });

    test('selects tariff from modal', async () => {
        const onSubmit = jest.fn();
        const {getByText, getAllByText, container} = renderWithFormContext({
            reduxStore: reduxStoreWithOpenedModal,
            onSubmit,
        });
        const openButton = getByText('Показать все тарифы');

        fireEvent.click(openButton);

        // Вариант 1 | Вариант 2 | Вариант 3
        // [Текущий] | [Выбрать] | [Выбрать]
        const selectButtons = await waitForElement(
            () => getAllByText('Выбрать'),
            {container},
        );

        fireEvent.click(selectButtons[1]);

        const form = container.querySelector('form')!;

        fireEvent.submit(form);

        expect(onSubmit).toHaveBeenCalledTimes(1);
        expect(onSubmit.mock.calls[0][0].flightInfo).toEqual({
            variantToken: variants[2].id,
        });
    });

    test('selected tariff is disabled', async () => {
        const {container, getByText} = renderWithFormContext({
            reduxStore: reduxStoreWithOpenedModal,
        });
        const openButton = getByText('Показать все тарифы');

        fireEvent.click(openButton);

        // Вариант 1 | Вариант 2 | Вариант 3
        // [Текущий] | [Выбрать] | [Выбрать]
        const selectButton = await waitForElement(() => getByText('Текущий'), {
            container,
        });

        expect(selectButton).toBeTruthy();
    });
});
