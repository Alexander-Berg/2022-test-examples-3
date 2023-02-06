import React, {useCallback, useState} from 'react';
import Cookie from 'js-cookie';

import {BUSES_TEST_CONTEXT_TOKEN_COOKIE_NAME} from 'constants/testContext';

import {ECardWithDeviceLayoutVariation} from 'components/CardWithDeviceLayout/types/ECardWithDeviceLayoutVariation';
import {
    EBusesBookOutcome,
    EBusesConfirmOutcome,
    EBusesRefundInfoOutcome,
    EBusesRefundOutcome,
    IBusesGetTestContextRequestParams,
} from 'server/api/BusesTravelApi/types/IBusesGetTestContext';

import {renderSelect} from 'projects/testControlPanel/utilities/renderSelect';
import convertEnumToSelectOptions from 'projects/testControlPanel/utilities/convertEnumToSelectOptions';
import {useDeviceType} from 'utilities/hooks/useDeviceType';
import {useAsyncState} from 'utilities/hooks/useAsyncState';
import {deviceMods} from 'utilities/stylesUtils';
import {getTestContextTokens} from 'projects/buses/utilities/testContext/getTestContextToken';
import initialValues from './utilities/initialValues';
import {renderInput} from 'projects/testControlPanel/utilities/renderInput';

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
import getOptionDescription from 'projects/testControlPanel/pages/TrainsPage/components/OrderPage/getOptionDescription';

import {busesBrowserProvider} from 'serviceProvider/buses/busesBrowserProvider';

import cx from './OrderPage.scss';

const OrderPage: React.FC = () => {
    const deviceType = useDeviceType();

    const uiState = useAsyncState();

    const [cookie, setCookie] = useState(getTestContextTokens() ?? '');

    const onSubmit = useCallback(
        (formValues: IBusesGetTestContextRequestParams) => {
            (async function (): Promise<void> {
                uiState.loading();

                try {
                    const res = await busesBrowserProvider.getTestContext(
                        formValues,
                    );

                    Cookie.set(
                        BUSES_TEST_CONTEXT_TOKEN_COOKIE_NAME,
                        res.testContextToken,
                    );

                    setCookie(res.testContextToken);

                    uiState.success();
                } catch (e) {
                    uiState.error();

                    return;
                }
            })();
        },
        [uiState],
    );

    const onReset = useCallback(() => {
        Cookie.remove(BUSES_TEST_CONTEXT_TOKEN_COOKIE_NAME);

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
                    <Form<IBusesGetTestContextRequestParams>
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
                                        name="bookOutcome"
                                        options={convertEnumToSelectOptions(
                                            EBusesBookOutcome,
                                        )}
                                        getOptionDescription={
                                            getOptionDescription
                                        }
                                        label="Результат создания бронирования"
                                    >
                                        {renderSelect}
                                    </Field>

                                    <Field
                                        name="confirmOutcome"
                                        options={convertEnumToSelectOptions(
                                            EBusesConfirmOutcome,
                                        )}
                                        getOptionDescription={
                                            getOptionDescription
                                        }
                                        label="Результат подтверждения бронирования"
                                    >
                                        {renderSelect}
                                    </Field>

                                    <Field
                                        name="refundInfoOutcome"
                                        options={convertEnumToSelectOptions(
                                            EBusesRefundInfoOutcome,
                                        )}
                                        getOptionDescription={
                                            getOptionDescription
                                        }
                                        label="Результат получения стоимости к возврату"
                                    >
                                        {renderSelect}
                                    </Field>

                                    <Field
                                        name="refundOutcome"
                                        options={convertEnumToSelectOptions(
                                            EBusesRefundOutcome,
                                        )}
                                        getOptionDescription={
                                            getOptionDescription
                                        }
                                        label="Результат проведения возврата"
                                    >
                                        {renderSelect}
                                    </Field>

                                    <Field
                                        name="expireAfterSeconds"
                                        label="Секунд до автоотмены брони по таймауту"
                                        inputType="number"
                                    >
                                        {renderInput}
                                    </Field>
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
                    className={cx('rightCard')}
                    variation={ECardWithDeviceLayoutVariation.ASIDE}
                >
                    <Heading level={2}>Текущее значения</Heading>

                    <Flex
                        flexDirection="column"
                        above={5}
                        below={5}
                        between={3}
                        style={{width: 300}}
                    >
                        <FieldLabel
                            label={BUSES_TEST_CONTEXT_TOKEN_COOKIE_NAME}
                        >
                            {cookie || '-'}
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

export default OrderPage;
