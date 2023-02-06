import React, {useCallback, useState} from 'react';
import Cookie from 'js-cookie';

import {TEST_TRAIN_SEARCH_COOKIE_NAME} from 'constants/testContext';

import {ECardWithDeviceLayoutVariation} from 'components/CardWithDeviceLayout/types/ECardWithDeviceLayoutVariation';
import {ETrainSearchMock} from 'projects/testControlPanel/pages/TrainsPage/types';

import {useDeviceType} from 'utilities/hooks/useDeviceType';
import {useAsyncState} from 'utilities/hooks/useAsyncState';
import {deviceMods} from 'utilities/stylesUtils';
import {getMockImSearchPath} from 'utilities/testUtils/getMockImSearchPath';

import CardWithDeviceLayout from 'components/CardWithDeviceLayout/CardWithDeviceLayout';
import Heading from 'components/Heading/Heading';
import Flex from 'components/Flex/Flex';
import Form from 'components/Form/Form';
import Button from 'components/Button/Button';
import Text from 'components/Text/Text';
import Spinner from 'components/Spinner/Spinner';
import {TwoColumnLayout} from 'components/Layouts/TwoColumnLayout/TwoColumnLayout';
import FieldLabel from 'components/FieldLabel/FieldLabel';

import cx from './SearchPage.scss';

const SearchPage: React.FC = () => {
    const deviceType = useDeviceType();

    const uiState = useAsyncState();

    const [cookie, setCookie] = useState(getMockImSearchPath() ?? '');

    const onSubmit = useCallback(() => {
        uiState.loading();

        try {
            Cookie.set(TEST_TRAIN_SEARCH_COOKIE_NAME, ETrainSearchMock.AUTO);

            setCookie(ETrainSearchMock.AUTO);

            uiState.success();
        } catch (e) {
            uiState.error();

            return;
        }
    }, [uiState]);

    const onReset = useCallback(() => {
        Cookie.remove(TEST_TRAIN_SEARCH_COOKIE_NAME);

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
                    <Form onSubmit={onSubmit}>
                        {({handleSubmit}): React.ReactNode => (
                            <form onSubmit={handleSubmit}>
                                <Text className={cx('title')} tag="div">
                                    Можно выставить автоматический мок на
                                    поиске. В этом случае для поисков Москва -
                                    Санкт-Петербург и обратно будут приходить
                                    только два замоканных поезда - 016А и 752А.
                                    Для остальных поездов цен не будет.
                                </Text>

                                <Flex
                                    alignItems="baseline"
                                    between={5}
                                    above={4}
                                    inline
                                >
                                    <Button
                                        theme="primary"
                                        type="submit"
                                        size="l"
                                    >
                                        Включить автоматический мок поиска
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
                    <Heading level={2}>Текущее значение</Heading>

                    <Flex
                        flexDirection="column"
                        above={5}
                        below={5}
                        between={3}
                        style={{width: 300}}
                    >
                        <FieldLabel label={TEST_TRAIN_SEARCH_COOKIE_NAME}>
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

export default SearchPage;
