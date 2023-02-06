import React, {useCallback} from 'react';

import {PAYMENT_TEST_CONTEXT_TOKEN_COOKIE_NAME} from 'constants/testContext';
import {EAppActions} from 'constants/platforms/TPlatforms';

import {ITestPaymentContextForm} from './types';
import {ECardWithDeviceLayoutVariation} from 'components/CardWithDeviceLayout/types/ECardWithDeviceLayoutVariation';
import {
    EPaymentFailureResponseCode,
    EPaymentOutcome,
} from 'server/api/OrdersAPI/types/TGetPaymentTestContextTokenParams';

import {renderSelect} from 'projects/testControlPanel/utilities/renderSelect';
import convertEnumToSelectOptions from 'projects/testControlPanel/utilities/convertEnumToSelectOptions';
import {useDeviceType} from 'utilities/hooks/useDeviceType';
import {useAsyncState} from 'utilities/hooks/useAsyncState';
import {renderInput} from 'projects/testControlPanel/utilities/renderInput';
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

import {useCoordinator} from 'contexts/PlatformContext';

import {orders as ordersProvider} from 'serviceProvider/orders/orders';

import initialValues from './initialValues';
import getOptionDescription from './getOptionDescription';

const TestContextPaymentPage: React.FC = () => {
    const deviceType = useDeviceType();

    const uiState = useAsyncState();

    const {
        value: paymentContextToken,
        setValue: setPaymentContextToken,
        clearValue: clearPaymentContextToken,
    } = useCookie(PAYMENT_TEST_CONTEXT_TOKEN_COOKIE_NAME);

    const coordinator = useCoordinator();

    const onSubmit = useCallback(
        (formValues: ITestPaymentContextForm) => {
            (async function (): Promise<void> {
                uiState.loading();

                try {
                    const {
                        paymentOutcome,
                        paymentFailureResponseCode,
                        paymentFailureResponseDescription,
                    } = formValues;

                    const params =
                        paymentOutcome === EPaymentOutcome.PO_SUCCESS
                            ? ({
                                  paymentOutcome,
                              } as const)
                            : ({
                                  paymentOutcome,
                                  paymentFailureResponseCode,
                                  paymentFailureResponseDescription,
                              } as const);

                    const res = await ordersProvider
                        .provider()
                        .getPaymentTestContextToken(params);

                    const paymentTestContextToken = res.token;

                    setPaymentContextToken(paymentTestContextToken);
                    coordinator.doAction(
                        EAppActions.SET_PAYMENT_TEST_CONTEXT,
                        paymentTestContextToken,
                    );

                    uiState.success();
                } catch (e) {
                    uiState.error();

                    return;
                }
            })();
        },
        [coordinator, setPaymentContextToken, uiState],
    );

    const onReset = useCallback(() => {
        clearPaymentContextToken();
        coordinator.doAction(EAppActions.SET_PAYMENT_TEST_CONTEXT, '');
    }, [clearPaymentContextToken, coordinator]);

    return (
        <TwoColumnLayout
            deviceType={deviceType}
            rightColumnOffset={10}
            rightColumnWidth={80}
        >
            <TwoColumnLayout.LeftColumn>
                <CardWithDeviceLayout below={5}>
                    <Form<ITestPaymentContextForm>
                        initialValues={initialValues}
                        onSubmit={onSubmit}
                    >
                        {({handleSubmit, values}): React.ReactNode => (
                            <form onSubmit={handleSubmit}>
                                <Flex
                                    below={8}
                                    flexDirection="column"
                                    between={5}
                                >
                                    <Field
                                        name="paymentOutcome"
                                        options={convertEnumToSelectOptions(
                                            EPaymentOutcome,
                                        )}
                                        getOptionDescription={
                                            getOptionDescription
                                        }
                                        label="Результат оплаты"
                                    >
                                        {renderSelect}
                                    </Field>
                                    {values.paymentOutcome ===
                                        EPaymentOutcome.PO_FAILURE && (
                                        <>
                                            <Field
                                                name="paymentFailureResponseCode"
                                                options={convertEnumToSelectOptions(
                                                    EPaymentFailureResponseCode,
                                                )}
                                                getOptionDescription={
                                                    getOptionDescription
                                                }
                                                label="Причина неудачи"
                                            >
                                                {renderSelect}
                                            </Field>
                                            <Field
                                                name="paymentFailureResponseDescription"
                                                label="Текстовое описание причины неудачи"
                                            >
                                                {renderInput}
                                            </Field>
                                        </>
                                    )}
                                </Flex>
                                <Flex alignItems="baseline" between={5} inline>
                                    <Button
                                        theme="primary"
                                        type="submit"
                                        size="l"
                                    >
                                        Запросить и сохранить токен
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
                    <Heading level={2}>Текущее значение</Heading>

                    <Flex
                        flexDirection="column"
                        above={5}
                        below={5}
                        between={3}
                        style={{width: 300}}
                    >
                        <FieldLabel
                            label={PAYMENT_TEST_CONTEXT_TOKEN_COOKIE_NAME}
                        >
                            {paymentContextToken || '-'}
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

export default TestContextPaymentPage;
