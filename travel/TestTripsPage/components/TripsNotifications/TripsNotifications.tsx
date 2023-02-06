import React, {useState} from 'react';
import Cookie from 'js-cookie';
import uniq from 'lodash/uniq';

import {MOCK_NOTIFICATIONS} from 'server/api/TripsApi/constants/mockCookieName';

import ETripNotificationType from 'types/trips/ITripNotifications/ETripNotificationType';

import parseNotificationsFromJSON from 'server/api/TripsApi/utilities/parseNotificationsFromJSON';

import Flex from 'components/Flex/Flex';
import Checkbox from 'components/Checkbox/Checkbox';

const TripsNotifications: React.FC = () => {
    const [selectedNotifications, setSelectedNotifications] = useState(
        parseNotificationsFromJSON(Cookie.get(MOCK_NOTIFICATIONS)),
    );

    return (
        <Flex inline between={4} alignItems="baseline">
            <div>Включить нотификации</div>
            <Flex flexDirection="column" between={2}>
                {Object.values(ETripNotificationType).map(notificationType => (
                    <Checkbox
                        key={notificationType}
                        checked={selectedNotifications.includes(
                            notificationType,
                        )}
                        onChange={(e): void => {
                            const v = e.target.checked;

                            if (v) {
                                setSelectedNotifications(state => {
                                    const newValue = uniq([
                                        ...state,
                                        notificationType,
                                    ]);

                                    Cookie.set(
                                        MOCK_NOTIFICATIONS,
                                        JSON.stringify(newValue),
                                    );

                                    return newValue;
                                });
                            } else {
                                setSelectedNotifications(state => {
                                    const newValue = state.filter(
                                        n => n !== notificationType,
                                    );

                                    Cookie.set(
                                        MOCK_NOTIFICATIONS,
                                        JSON.stringify(newValue),
                                    );

                                    return newValue;
                                });
                            }
                        }}
                        label={notificationType}
                    />
                ))}
            </Flex>
        </Flex>
    );
};

export default TripsNotifications;
