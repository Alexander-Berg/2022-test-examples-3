// @ts-nocheck

import React from 'react';
import { create } from 'react-test-renderer';

import Settings from '../index';

test.skip('snapshot', () => {
    const c = create(
        <Settings
            region={{
                activeCity: {
                    id: 1,
                    name: 'Moscow',
                },
                activeCountry: {
                    id: 1,
                    name: 'Russia',
                },
            }}
            setRegion={() => true}
            showOffersFromOtherRegions
            setShowOffersFromOtherRegions={() => true}
            showPopupOnHover
            setShowPopupOnHover={() => true}
            showProductNotifications
            setShowProductNotifications={() => true}
            showAviaNotifications
            setShowAviaNotifications={() => true}
            showAutoNotifications
            setShowAutoNotifications={() => true}
            saveCityButtonVisible={false}
            setSaveCityButtonVisible={() => true}
            isCitySet={false}
            setCityState={() => true}
        />,
    );
    expect(c.toJSON()).toMatchSnapshot();
});
