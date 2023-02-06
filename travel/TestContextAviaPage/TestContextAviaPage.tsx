import React, {useCallback} from 'react';

import {EAppActions} from 'constants/platforms/TPlatforms';

import {
    ECheckAvailabilityBeforeBookingOutcome,
    ECheckAvailabilityOnRedirOutcome,
    EConfirmationOutcome,
    EMqEventOutcome,
    ETokenizationOutcome,
} from 'server/api/AviaBookingApi/types/IAviaTestContextTokenApiParams';
import {ITestAviaContextForm} from './types';
import {ECardWithDeviceLayoutVariation} from 'components/CardWithDeviceLayout/types/ECardWithDeviceLayoutVariation';

import {renderSelect} from 'projects/testControlPanel/utilities/renderSelect';
import {renderCheckbox} from 'projects/testControlPanel/utilities/renderCheckbox';
import convertEnumToSelectOptions from 'projects/testControlPanel/utilities/convertEnumToSelectOptions';
import {useDeviceType} from 'utilities/hooks/useDeviceType';
import {useAsyncState} from 'utilities/hooks/useAsyncState';
import {renderTextarea} from 'projects/testControlPanel/utilities/renderTextarea';
import {useCookie} from 'utilities/hooks/useCookie';

import CardWithDeviceLayout from 'components/CardWithDeviceLayout/CardWithDeviceLayout';
import Heading from 'components/Heading/Heading';
import Flex from 'components/Flex/Flex';
import Form from 'components/Form/Form';
import Field from 'components/Form/components/Field/Field';
import Button from 'components/Button/Button';
import Text from 'components/Text/Text';
import Spinner from 'components/Spinner/Spinner';
import {TwoColumnLayout} from 'components/Layouts/TwoColumnLayout/TwoColumnLayout';
import FieldLabel from 'components/FieldLabel/FieldLabel';

import {bookingApiClient} from 'projects/avia/pages/AviaBooking/api/bookingApiClient';
import getOptionDescription from 'projects/testControlPanel/pages/TestContextAviaPage/getOptionDescription';

import {useCoordinator} from 'contexts/PlatformContext';

import initialValues from './initialValues';
import minifyJSON from './minifyJSON';
import validateJSON from './validateJSON';
import exampleAviaVariants from './exampleAviaVariants';

const TestContextAviaPage: React.FC = () => {
    const deviceType = useDeviceType();

    const uiState = useAsyncState();

    const {
        value: variantTestContext,
        setValue: setVariantTestContext,
        clearValue: clearVariantTestContext,
    } = useCookie('variantTestContext');
    const {
        value: paymentTestContext,
        setValue: setPaymentTestContext,
        clearValue: clearPaymentTestContext,
    } = useCookie('paymentTestContext');

    const coordinator = useCoordinator();

    const onSubmit = useCallback(
        (formValues: ITestAviaContextForm) => {
            (async function (): Promise<void> {
                const {skipPayment, aviaVariants, ...contextParams} =
                    formValues;

                uiState.loading();

                try {
                    const res = await bookingApiClient.getTestContextToken({
                        ...contextParams,
                        aviaVariants: aviaVariants
                            ? minifyJSON(aviaVariants)
                            : undefined,
                        mockAviaVariants: Boolean(aviaVariants),
                    });

                    const newVariantTestContext = res.token;
                    const newPaymentTestContext = skipPayment
                        ? res.paymentToken
                        : '';

                    setVariantTestContext(newVariantTestContext);
                    setPaymentTestContext(newPaymentTestContext);

                    coordinator.doAction(EAppActions.SET_AVIA_TEST_CONTEXT, {
                        variantTestContext: newVariantTestContext,
                        paymentTestContext: newPaymentTestContext,
                    });

                    uiState.success();
                } catch (e) {
                    uiState.error();

                    return;
                }
            })();
        },
        [coordinator, setPaymentTestContext, setVariantTestContext, uiState],
    );

    const onReset = useCallback(() => {
        clearVariantTestContext();
        clearPaymentTestContext();

        coordinator.doAction(EAppActions.SET_AVIA_TEST_CONTEXT, {
            variantTestContext: '',
            paymentTestContext: '',
        });
    }, [clearPaymentTestContext, clearVariantTestContext, coordinator]);

    return (
        <TwoColumnLayout
            deviceType={deviceType}
            rightColumnOffset={10}
            rightColumnWidth={80}
        >
            <TwoColumnLayout.LeftColumn>
                <CardWithDeviceLayout below={5}>
                    <Form<ITestAviaContextForm>
                        initialValues={initialValues}
                        onSubmit={onSubmit}
                    >
                        {({handleSubmit, form}): React.ReactNode => (
                            <form onSubmit={handleSubmit}>
                                <Flex
                                    below={8}
                                    flexDirection="column"
                                    between={5}
                                >
                                    <Field
                                        name="checkAvailabilityOnRedirOutcome"
                                        options={convertEnumToSelectOptions(
                                            ECheckAvailabilityOnRedirOutcome,
                                        )}
                                        getOptionDescription={
                                            getOptionDescription
                                        }
                                        label="Результат проверки доступности варианта при редиректе на BoY"
                                    >
                                        {renderSelect}
                                    </Field>
                                    <Field
                                        name="checkAvailabilityBeforeBookingOutcome"
                                        options={convertEnumToSelectOptions(
                                            ECheckAvailabilityBeforeBookingOutcome,
                                        )}
                                        getOptionDescription={
                                            getOptionDescription
                                        }
                                        label="Результат проверки доступности варианта перед созданием заказа"
                                    >
                                        {renderSelect}
                                    </Field>
                                    <Field
                                        name="tokenizationOutcome"
                                        options={convertEnumToSelectOptions(
                                            ETokenizationOutcome,
                                        )}
                                        getOptionDescription={
                                            getOptionDescription
                                        }
                                        label="Результат токенизации карты"
                                    >
                                        {renderSelect}
                                    </Field>
                                    <Field
                                        name="confirmationOutcome"
                                        options={convertEnumToSelectOptions(
                                            EConfirmationOutcome,
                                        )}
                                        getOptionDescription={
                                            getOptionDescription
                                        }
                                        label="Результат подтверждения бронирования заказа"
                                    >
                                        {renderSelect}
                                    </Field>
                                    <Field
                                        name="mqEventOutcome"
                                        options={convertEnumToSelectOptions(
                                            EMqEventOutcome,
                                        )}
                                        getOptionDescription={
                                            getOptionDescription
                                        }
                                        label="Тип MQ события, присылаемого аэрофлотом о статусе заказа"
                                    >
                                        {renderSelect}
                                    </Field>
                                    <Field
                                        name="aviaVariants"
                                        label="Замокать авиа выдачу"
                                        validate={validateJSON}
                                        helpLink="https://st.yandex-team.ru/RASPTICKETS-21483#61fd18b698682257e2b4f327"
                                        onFillButtonClick={(): void => {
                                            form.change(
                                                'aviaVariants',
                                                JSON.stringify(
                                                    exampleAviaVariants,
                                                    undefined,
                                                    2,
                                                ),
                                            );
                                        }}
                                    >
                                        {renderTextarea}
                                    </Field>
                                    <Field
                                        name="skipPayment"
                                        label="Пропустить оплату"
                                    >
                                        {renderCheckbox}
                                    </Field>
                                </Flex>
                                <Flex alignItems="baseline" between={5} inline>
                                    <Button
                                        theme="primary"
                                        type="submit"
                                        size="l"
                                    >
                                        Запросить и сохранить токены
                                    </Button>
                                    {uiState.isError && (
                                        <Text color="alert">
                                            Произошла ошибка
                                        </Text>
                                    )}
                                    {uiState.isLoading && <Spinner size="xs" />}
                                </Flex>
                            </form>
                        )}
                    </Form>
                </CardWithDeviceLayout>
            </TwoColumnLayout.LeftColumn>
            <TwoColumnLayout.RightColumn>
                <CardWithDeviceLayout
                    variation={ECardWithDeviceLayoutVariation.ASIDE}
                >
                    <Heading level={2}>Текущие значения</Heading>

                    <Flex
                        flexDirection="column"
                        above={5}
                        below={5}
                        between={3}
                        style={{width: 300}}
                    >
                        <FieldLabel label="variantTestContext">
                            {variantTestContext || '-'}
                        </FieldLabel>
                        <FieldLabel label="paymentTestContext">
                            {paymentTestContext || '-'}
                        </FieldLabel>
                    </Flex>

                    <Button size="l" onClick={onReset}>
                        Сбросить
                    </Button>
                </CardWithDeviceLayout>
            </TwoColumnLayout.RightColumn>
        </TwoColumnLayout>
    );
};

export default TestContextAviaPage;
