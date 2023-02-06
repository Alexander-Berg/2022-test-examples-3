import React, {ChangeEventHandler, useCallback} from 'react';

import {
    MOCK_ACTIVE_TRIPS_COUNT,
    MOCK_COOKIE_NAME,
    MOCK_PAST_TRIPS_COUNT,
    MOCK_NOTIFICATIONS,
    MOCK_RESTRICTIONS,
    MOCK_FORECAST,
} from 'server/api/TripsApi/constants/mockCookieName';

import {useCookie} from 'utilities/hooks/useCookie';

import Checkbox from 'components/Checkbox/Checkbox';
import CardWithDeviceLayout from 'components/CardWithDeviceLayout/CardWithDeviceLayout';
import Flex from 'components/Flex/Flex';
import FieldLabel from 'components/FieldLabel/FieldLabel';
import Input from 'components/Input/Input';
import TripsNotifications from 'projects/testControlPanel/pages/TestTripsPage/components/TripsNotifications/TripsNotifications';
import ActivityTypes from 'projects/testControlPanel/pages/TestTripsPage/components/ActivityTypes/ActivityTypes';

import cx from './TestTripsPage.scss';

const TestTripsPage: React.FC = () => {
    const {value: mockTrips, setValue: setMockTrips} =
        useCookie(MOCK_COOKIE_NAME);

    const {
        value: activeTripsCount,
        setValue: setActiveTripsCount,
        clearValue: clearActiveTripsCount,
    } = useCookie(MOCK_ACTIVE_TRIPS_COUNT);
    const {
        value: pastTripsCount,
        setValue: setPastTripsCount,
        clearValue: clearPastTripsCount,
    } = useCookie(MOCK_PAST_TRIPS_COUNT);
    const {
        value: mockRestrictions,
        setValue: setMockRestrictions,
        clearValue: clearMockRestrictions,
    } = useCookie(MOCK_RESTRICTIONS);
    const {clearValue: clearMockNotifications} = useCookie(MOCK_NOTIFICATIONS);
    const {
        value: forecastCount,
        setValue: setForecastCount,
        clearValue: clearForecastCount,
    } = useCookie(MOCK_FORECAST);

    const handleChange: ChangeEventHandler<HTMLInputElement> = useCallback(
        e => {
            const v = e.target.checked;

            setMockTrips(String(v));

            if (!v) {
                clearActiveTripsCount();
                clearPastTripsCount();
                clearMockNotifications();
                clearMockRestrictions();
                clearForecastCount();
            }
        },
        [
            clearActiveTripsCount,
            clearForecastCount,
            clearMockNotifications,
            clearMockRestrictions,
            clearPastTripsCount,
            setMockTrips,
        ],
    );

    return (
        <CardWithDeviceLayout>
            <Flex flexDirection="column" between={2}>
                <Checkbox
                    checked={mockTrips === 'true'}
                    onChange={handleChange}
                    label="Использовать замоканные данные для поездок (trips-api)"
                />
                {mockTrips && (
                    <Flex flexDirection="column" between={4}>
                        <FieldLabel label="Количество активных поездок">
                            <Input
                                className={cx('input')}
                                type="number"
                                placeholder="все активные поездки из мока"
                                value={activeTripsCount}
                                onChange={(e): void => {
                                    setActiveTripsCount(e.target.value);
                                }}
                            />
                        </FieldLabel>
                        <FieldLabel label="Количество прошедших поездок">
                            <Input
                                className={cx('input')}
                                type="number"
                                placeholder="все прошедшие поездки из мока"
                                value={pastTripsCount}
                                onChange={(e): void => {
                                    setPastTripsCount(e.target.value);
                                }}
                            />
                        </FieldLabel>
                        <FieldLabel label="Количество элементов в прогнозе">
                            <Input
                                className={cx('input')}
                                type="number"
                                placeholder="все элементы прогноза из мока"
                                value={forecastCount}
                                onChange={(e): void => {
                                    setForecastCount(e.target.value);
                                }}
                            />
                        </FieldLabel>
                        <Checkbox
                            checked={mockRestrictions === 'true'}
                            onChange={(e): void => {
                                setMockRestrictions(String(e.target.checked));
                            }}
                            label="Использовать замоканные ограничения"
                        />
                        <TripsNotifications />
                        <ActivityTypes />
                    </Flex>
                )}
            </Flex>
        </CardWithDeviceLayout>
    );
};

export default TestTripsPage;
