import { shallow } from 'enzyme';
import * as React from 'react';

import { B2B_REQUESTS } from '../../request';
import { SearchWallets } from '../component';
import {
    walletLimitEmptyRequest,
    walletLimitErrorRequest,
    walletLimitOnlyRequest,
    walletLimitOnlyResult,
    walletLimitSeveralValidRequest,
    walletLimitSeveralValidResult,
    walletLimitValidRequest,
    walletLimitValidResult,
} from './mock';

const location = {
    search: "",
};

describe('searchWalletLimit', () => {
    it('should return valid result', async () => {
        const searchWallets = shallow(
            <SearchWallets location={location}/>,
        );
        const instance: any = searchWallets.instance();
        expect(await instance.searchWalletLimit(
            '123456789',
            new walletLimitValidRequest({ requestConfigs: B2B_REQUESTS }),
            jest.fn,
        )).toMatchObject(walletLimitValidResult);
    });

    it('should return null when error', async () => {
        const searchWallets = shallow(
            <SearchWallets location={location}/>,
        );
        const instance: any = searchWallets.instance();
        expect(await instance.searchWalletLimit(
            '123456789',
            new walletLimitErrorRequest({ requestConfigs: B2B_REQUESTS }),
            jest.fn,
        )).toBeNull();
    });

    it('should return only walletLimits', async () => {
        const searchWallets = shallow(
            <SearchWallets location={location}/>,
        );
        const instance: any = searchWallets.instance();
        expect(await instance.searchWalletLimit(
            '123456789',
            new walletLimitOnlyRequest({ requestConfigs: B2B_REQUESTS }),
            jest.fn,
        )).toMatchObject(walletLimitOnlyResult);
    });

    it('should return empty result', async () => {
        const searchWallets = shallow(
            <SearchWallets location={location}/>,
        );
        const instance: any = searchWallets.instance();
        expect(await instance.searchWalletLimit(
            '123456789',
            new walletLimitEmptyRequest({ requestConfigs: B2B_REQUESTS }),
            jest.fn,
        )).toMatchObject({ walletLimits: [], walletAccounts: {} });
    });
});

describe('searchOrganizationsById', () => {
    it('should return valid result - several wallets (ex: 2350)', async () => {
        const searchWallets = shallow(
            <SearchWallets location={location}/>,
        );
        const instance: any = searchWallets.instance();
        expect(await instance.searchOrganizationsById(
            '123456789',
            new walletLimitSeveralValidRequest({ requestConfigs: B2B_REQUESTS }),
            jest.fn,
        )).toMatchObject(walletLimitSeveralValidResult);
    });

    it('should return valid result', async () => {
        const searchWallets = shallow(
            <SearchWallets location={location}/>,
        );
        const instance: any = searchWallets.instance();
        expect(await instance.searchOrganizationsById(
            '123456789',
            new walletLimitValidRequest({ requestConfigs: B2B_REQUESTS }),
            jest.fn,
        )).toMatchObject(walletLimitValidResult);
    });

    it('should return null when error', async () => {
        const searchWallets = shallow(
            <SearchWallets location={location}/>,
        );
        const instance: any = searchWallets.instance();
        expect(await instance.searchOrganizationsById(
            '123456789',
            new walletLimitErrorRequest({ requestConfigs: B2B_REQUESTS }),
            jest.fn,
        )).toBeNull();
    });

    it('should return only walletLimits', async () => {
        const searchWallets = shallow(
            <SearchWallets location={location}/>,
        );
        const instance: any = searchWallets.instance();
        expect(await instance.searchOrganizationsById(
            '123456789',
            new walletLimitOnlyRequest({ requestConfigs: B2B_REQUESTS }),
            jest.fn,
        )).toMatchObject(walletLimitOnlyResult);
    });

    it('should return empty result', async () => {
        const searchWallets = shallow(
            <SearchWallets location={location}/>,
        );
        const instance: any = searchWallets.instance();
        expect(await instance.searchOrganizationsById(
            '123456789',
            new walletLimitEmptyRequest({ requestConfigs: B2B_REQUESTS }),
            jest.fn,
        )).toMatchObject({ organizations: {}, walletLimits: [], walletAccounts: {} });
    });
});
