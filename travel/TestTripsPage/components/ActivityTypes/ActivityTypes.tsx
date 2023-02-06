import React, {useState} from 'react';
import Cookie from 'js-cookie';
import uniq from 'lodash/uniq';

import {MOCK_ACTIVITY_TYPES} from 'server/api/TripsApi/constants/mockCookieName';

import ETripActivityType from 'types/trips/ITripActivities/ETripActivityType';

import parseActivityTypesFromJSON from 'server/api/TripsApi/utilities/parseActivityTypesFromJSON';

import Flex from 'components/Flex/Flex';
import Checkbox from 'components/Checkbox/Checkbox';

const ActivityTypes: React.FC = () => {
    const [enabled, setEnabled] = useState(
        Boolean(Cookie.get(MOCK_ACTIVITY_TYPES)),
    );
    const [selectedActivityTypes, setSelectedActivityTypes] = useState(
        parseActivityTypesFromJSON(Cookie.get(MOCK_ACTIVITY_TYPES)),
    );

    return (
        <Flex inline between={4} alignItems="baseline">
            <div>
                <Checkbox
                    checked={enabled}
                    onChange={(e): void => {
                        const v = e.target.checked;

                        setSelectedActivityTypes([]);
                        setEnabled(v);

                        if (v) {
                            Cookie.set(MOCK_ACTIVITY_TYPES, JSON.stringify([]));
                        } else {
                            Cookie.remove(MOCK_ACTIVITY_TYPES);
                        }
                    }}
                    label="Фильтровать доступные виды активностей"
                />
            </div>
            {enabled && (
                <Flex flexDirection="column" between={2}>
                    {Object.values(ETripActivityType).map(activityType => (
                        <Checkbox
                            key={activityType}
                            checked={selectedActivityTypes.includes(
                                activityType,
                            )}
                            onChange={(e): void => {
                                const v = e.target.checked;

                                if (v) {
                                    setSelectedActivityTypes(state => {
                                        const newValue = uniq([
                                            ...state,
                                            activityType,
                                        ]);

                                        Cookie.set(
                                            MOCK_ACTIVITY_TYPES,
                                            JSON.stringify(newValue),
                                        );

                                        return newValue;
                                    });
                                } else {
                                    setSelectedActivityTypes(state => {
                                        const newValue = state.filter(
                                            n => n !== activityType,
                                        );

                                        Cookie.set(
                                            MOCK_ACTIVITY_TYPES,
                                            JSON.stringify(newValue),
                                        );

                                        return newValue;
                                    });
                                }
                            }}
                            label={activityType}
                        />
                    ))}
                </Flex>
            )}
        </Flex>
    );
};

export default ActivityTypes;
