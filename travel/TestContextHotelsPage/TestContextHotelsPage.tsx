import React, {useCallback, useState} from 'react';
import omitBy from 'lodash/omitBy';

import {ITestHotelsContextForm} from './types';
import {ECardWithDeviceLayoutVariation} from 'components/CardWithDeviceLayout/types/ECardWithDeviceLayoutVariation';
import {ITestBookOfferTokenResponse} from 'server/api/HotelsBookAPI/types/ITestBookOfferToken';

import {useDeviceType} from 'utilities/hooks/useDeviceType';
import {useAsyncState} from 'utilities/hooks/useAsyncState';
import {deviceMods} from 'utilities/stylesUtils';

import CardWithDeviceLayout from 'components/CardWithDeviceLayout/CardWithDeviceLayout';
import Flex from 'components/Flex/Flex';
import FormContent from './components/FormContent/FormContent';
import Button from 'components/Button/Button';
import Text from 'components/Text/Text';
import Spinner from 'components/Spinner/Spinner';
import {TwoColumnLayout} from 'components/Layouts/TwoColumnLayout/TwoColumnLayout';
import Form from 'components/Form/Form';
import OfferList from 'projects/testControlPanel/pages/TestContextHotelsPage/components/OfferList/OfferList';

import {hotelBookService} from 'serviceProvider';

import initialValues from './initialValues';

import cx from './TestContextHotelsPage.scss';

const TestContextHotelsPage: React.FC = () => {
    const deviceType = useDeviceType();

    const uiState = useAsyncState();
    const [offers, setOffers] = useState<
        ITestBookOfferTokenResponse['data'] | null
    >(null);

    const onSubmit = useCallback(
        (formValues: ITestHotelsContextForm) => {
            (async function (): Promise<void> {
                uiState.loading();

                try {
                    const filteredFormValues = omitBy(formValues, value => {
                        return (
                            value === '' || value === 0 || value === undefined
                        );
                    });
                    const res = await hotelBookService
                        .provider()
                        .getHotelsTestContextToken(filteredFormValues);

                    setOffers(res.data);

                    uiState.success();
                } catch (e) {
                    console.error(e);
                    uiState.error();

                    return;
                }
            })();
        },
        [uiState],
    );

    return (
        <Form<ITestHotelsContextForm>
            initialValues={initialValues}
            onSubmit={onSubmit}
        >
            {({handleSubmit}): React.ReactNode => (
                <form onSubmit={handleSubmit}>
                    <TwoColumnLayout
                        className={cx('root', deviceMods('root', deviceType))}
                        deviceType={deviceType}
                        rightColumnOffset={10}
                        rightColumnWidth={80}
                    >
                        <TwoColumnLayout.LeftColumn>
                            <CardWithDeviceLayout below={5}>
                                {offers ? (
                                    <OfferList
                                        offerTokens={offers.offerTokens}
                                    />
                                ) : (
                                    <FormContent />
                                )}
                            </CardWithDeviceLayout>
                        </TwoColumnLayout.LeftColumn>
                        <TwoColumnLayout.RightColumn>
                            <CardWithDeviceLayout
                                className={cx('rightCard')}
                                variation={ECardWithDeviceLayoutVariation.ASIDE}
                            >
                                {offers ? (
                                    <Button
                                        theme="primary"
                                        type="submit"
                                        size="l"
                                        width="max"
                                        onClick={(): void => {
                                            setOffers(null);
                                        }}
                                    >
                                        Назад
                                    </Button>
                                ) : (
                                    <Flex flexDirection="column" between={2}>
                                        <Button
                                            theme="primary"
                                            type="submit"
                                            size="l"
                                            width="max"
                                            disabled={uiState.isLoading}
                                        >
                                            <Flex inline between={2}>
                                                <span>Получить офферы</span>
                                                <div
                                                    className={cx(
                                                        'spinnerWrapper',
                                                    )}
                                                >
                                                    {uiState.isLoading && (
                                                        <Spinner size="xxs" />
                                                    )}
                                                </div>
                                            </Flex>
                                        </Button>
                                        {uiState.isError && (
                                            <Text color="alert">
                                                Произошла ошибка
                                            </Text>
                                        )}
                                    </Flex>
                                )}
                            </CardWithDeviceLayout>
                        </TwoColumnLayout.RightColumn>
                    </TwoColumnLayout>
                </form>
            )}
        </Form>
    );
};

export default TestContextHotelsPage;
