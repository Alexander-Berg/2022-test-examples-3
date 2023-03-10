import React, {useCallback, useState} from 'react';
import Cookie from 'js-cookie';

import {TRAIN_TEST_CONTEXT_TOKEN_COOKIE_NAME} from 'constants/testContext';

import {ITestTrainsContextForm} from './types';
import {ECardWithDeviceLayoutVariation} from 'components/CardWithDeviceLayout/types/ECardWithDeviceLayoutVariation';
import {
    EConfirmReservationOutcome,
    ECreateReservationOutcome,
    EInsuranceCheckoutConfirmOutcome,
    EInsuranceCheckoutOutcome,
    EInsurancePricingOutcome,
    ERefundCheckoutOutcome,
    ERefundPricingOutcome,
} from 'server/api/TrainsBookingApi/types/ITrainsTestContextToken';

import {renderSelect} from 'projects/testControlPanel/utilities/renderSelect';
import convertEnumToSelectOptions from 'projects/testControlPanel/utilities/convertEnumToSelectOptions';
import {useDeviceType} from 'utilities/hooks/useDeviceType';
import {useAsyncState} from 'utilities/hooks/useAsyncState';
import {renderInput} from 'projects/testControlPanel/utilities/renderInput';
import {deviceMods} from 'utilities/stylesUtils';
import {renderCheckbox} from 'projects/testControlPanel/utilities/renderCheckbox';

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

import {trains as trainsProvider} from 'serviceProvider/trains/trains';

import initialValues from './initialValues';
import getOptionDescription from './getOptionDescription';

import cx from './OrderPage.scss';

const OrderPage: React.FC = () => {
    const deviceType = useDeviceType();

    const uiState = useAsyncState();

    const [cookie, setCookie] = useState(
        Cookie.get(TRAIN_TEST_CONTEXT_TOKEN_COOKIE_NAME) ?? '',
    );

    const onSubmit = useCallback(
        (formValues: ITestTrainsContextForm) => {
            (async function (): Promise<void> {
                uiState.loading();

                try {
                    const {setOnlyForSecondTrain, ...params} = formValues;
                    const res = await trainsProvider
                        .provider()
                        .testContextToken(params);

                    const nextToken = res.test_context_token;
                    const testContextToken =
                        cookie && setOnlyForSecondTrain
                            ? [cookie, nextToken].join('&')
                            : nextToken;

                    Cookie.set(
                        TRAIN_TEST_CONTEXT_TOKEN_COOKIE_NAME,
                        testContextToken,
                    );

                    setCookie(testContextToken);

                    uiState.success();
                } catch (e) {
                    uiState.error();

                    return;
                }
            })();
        },
        [cookie, uiState],
    );

    const onReset = useCallback(() => {
        Cookie.remove(TRAIN_TEST_CONTEXT_TOKEN_COOKIE_NAME);

        setCookie('');
    }, []);

    return (
        <TwoColumnLayout
            className={cx('root', deviceMods('root', deviceType))}
            deviceType={deviceType}
            rightColumnOffset={10}
            rightColumnWidth={80}
        >
            <TwoColumnLayout.LeftColumn>
                <CardWithDeviceLayout below={5}>
                    <Form<ITestTrainsContextForm>
                        initialValues={initialValues}
                        onSubmit={onSubmit}
                    >
                        {({handleSubmit}): React.ReactNode => (
                            <form onSubmit={handleSubmit}>
                                <Flex
                                    below={8}
                                    flexDirection="column"
                                    between={5}
                                >
                                    <Field
                                        name="insurancePricingOutcome"
                                        options={convertEnumToSelectOptions(
                                            EInsurancePricingOutcome,
                                        )}
                                        getOptionDescription={
                                            getOptionDescription
                                        }
                                        label="?????????????????? ?????????????????? ???????? ??????????????????"
                                    >
                                        {renderSelect}
                                    </Field>
                                    <Field
                                        name="insuranceCheckoutOutcome"
                                        options={convertEnumToSelectOptions(
                                            EInsuranceCheckoutOutcome,
                                        )}
                                        getOptionDescription={
                                            getOptionDescription
                                        }
                                        label="?????????????????? ?????????????????????? ??????????????????"
                                    >
                                        {renderSelect}
                                    </Field>
                                    <Field
                                        name="insuranceCheckoutConfirmOutcome"
                                        options={convertEnumToSelectOptions(
                                            EInsuranceCheckoutConfirmOutcome,
                                        )}
                                        getOptionDescription={
                                            getOptionDescription
                                        }
                                        label="?????????????????? ?????????????????????????? ???????????????????? ?????????????????? (?????????? ????????????)"
                                    >
                                        {renderSelect}
                                    </Field>
                                    <Field
                                        name="refundPricingOutcome"
                                        options={convertEnumToSelectOptions(
                                            ERefundPricingOutcome,
                                        )}
                                        getOptionDescription={
                                            getOptionDescription
                                        }
                                        label="?????????????????? ?????????????????? ?????????????????? ?? ????????????????"
                                    >
                                        {renderSelect}
                                    </Field>
                                    <Field
                                        name="refundCheckoutOutcome"
                                        options={convertEnumToSelectOptions(
                                            ERefundCheckoutOutcome,
                                        )}
                                        getOptionDescription={
                                            getOptionDescription
                                        }
                                        label="?????????????????? ???????????????????? ????????-????????????????"
                                    >
                                        {renderSelect}
                                    </Field>
                                    <Field
                                        name="createReservationOutcome"
                                        options={convertEnumToSelectOptions(
                                            ECreateReservationOutcome,
                                        )}
                                        getOptionDescription={
                                            getOptionDescription
                                        }
                                        label="?????????????????? ???????????????? ????????????????????????"
                                    >
                                        {renderSelect}
                                    </Field>
                                    <Field
                                        name="confirmReservationOutcome"
                                        options={convertEnumToSelectOptions(
                                            EConfirmReservationOutcome,
                                        )}
                                        getOptionDescription={
                                            getOptionDescription
                                        }
                                        label="?????????????????? ?????????????????????????? ????????????????????????"
                                    >
                                        {renderSelect}
                                    </Field>
                                    <Field
                                        name="officeReturnDelayInSeconds"
                                        label="???????????? ???????????????? ???????????? ?????????? ?????????? (?? ????????????????)"
                                        message="???????? ???? ???????????? ?????? 0 - ???????????????? ???? ??????????. ?????????????????????? ?????? ?????????????????? - ?????????? ?????????????? ???? ???????????? 10 ????????????"
                                        inputType="number"
                                    >
                                        {renderInput}
                                    </Field>
                                    <Field
                                        name="officeAcquireDelayInSeconds"
                                        label="???????????? ?????????????????? ???????????? ?? ?????????? (?? ????????????????)"
                                        message="???????? ???? ???????????? ?????? 0 - ?????????? ???? ???????????? ???????? ?????????????? ?? ??????????"
                                        inputType="number"
                                    >
                                        {renderInput}
                                    </Field>
                                    <Field
                                        name="alwaysTimeoutAfterConfirmingInSeconds"
                                        label="???????????? ?????????????? ?????????? ???????????????? ???????????? ???? ???????????????????? ???????????? ?? ???? ?????????? ?????????????????????? ?????????????????? (?? ????????????????)"
                                        message="???????? ???? ???????????? ?????? 0 - ???????????????? ???? ??????????"
                                        inputType="number"
                                    >
                                        {renderInput}
                                    </Field>
                                    {Boolean(cookie) && (
                                        <Field
                                            name="setOnlyForSecondTrain"
                                            label="?????????????????? ?????? ?????????????? ???????????? (?????????????? ??????????)"
                                        >
                                            {renderCheckbox}
                                        </Field>
                                    )}
                                </Flex>
                                <Flex alignItems="baseline" between={5} inline>
                                    <Button
                                        theme="primary"
                                        type="submit"
                                        size="l"
                                    >
                                        ?????????????????? ?? ?????????????????? ??????????
                                    </Button>
                                    {uiState.isError && (
                                        <Text color="alert">
                                            ?????????????????? ????????????
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
                    className={cx('rightCard')}
                    variation={ECardWithDeviceLayoutVariation.ASIDE}
                >
                    <Heading level={2}>?????????????? ????????????????</Heading>

                    <Flex
                        flexDirection="column"
                        above={5}
                        below={5}
                        between={3}
                        style={{width: 300}}
                    >
                        <FieldLabel
                            label={TRAIN_TEST_CONTEXT_TOKEN_COOKIE_NAME}
                        >
                            {cookie || '-'}
                        </FieldLabel>
                    </Flex>

                    <Button size="l" onClick={onReset}>
                        ????????????????
                    </Button>
                </CardWithDeviceLayout>
            </TwoColumnLayout.RightColumn>
        </TwoColumnLayout>
    );
};

export default OrderPage;
