import { mount, ReactWrapper } from 'enzyme';
import React from 'react';
import { Provider } from 'react-redux';
import configureStore, { MockStore } from 'redux-mock-store';

import { mockStoreData } from '../../test-data/storeData';
import { mockSummary } from './Summary.mock';

import { SummaryConnected } from './Summary.container';

const mockStore = configureStore();

const campaignId = 'campaign-id';
const emptyCampaignId = 'empty-campaign-id';
const sampleCampaignId = 'sample-campaign-id';
const setCampaign = jest.fn();

let store: MockStore;
let component: ReactWrapper;

describe('Dispenser Summary', () => {
    beforeEach(() => {
        store = mockStore({
            ...mockStoreData,
            summaries: {
                [emptyCampaignId]: {
                    bigOrders: [],
                    clouds: [],
                },
                [sampleCampaignId]: {
                    ...mockSummary,
                }
            },
        });
    });

    afterEach((): void => {
        component.unmount();
    });

    it('should perform fetch trigger action on empty state with campaign id', () => {
        const actions = store.getActions();

        component = mount(
            <Provider store={store}>
                <SummaryConnected
                    campaignId={campaignId}
                    setCampaign={setCampaign}
                />
            </Provider>
        );

        expect(actions.length).toEqual(1);
        expect(actions[0].payload).toEqual(campaignId);
    });

    it('should not perform fetch when summary for campaign presented', () => {
        const actions = store.getActions();

        component = mount(
            <Provider store={store}>
                <SummaryConnected
                    campaignId={sampleCampaignId}
                    setCampaign={setCampaign}
                />
            </Provider>
        );

        expect(actions.length).toEqual(0);
    });

    it('should display message when no cloud information presented', () => {
        component = mount(
            <Provider store={store}>
                <SummaryConnected
                    campaignId={emptyCampaignId}
                    setCampaign={setCampaign}
                />
            </Provider>
        );

        const message = component.find('Message');

        expect(message.text()).toEqual('i18n:no-information');
    });
});
