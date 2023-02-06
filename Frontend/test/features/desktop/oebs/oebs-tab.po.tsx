import React from 'react';
import { render as libRender } from '@testing-library/react';

import { User } from '~/src/common/context/types';
import { withContext } from '~/src/common/hoc';

import OebsTabConnected from '~/src/features/Oebs/Oebs.container';

import { configureStore, StoreOptions } from '~/src/abc/react/redux/store';
import { withRedux } from '~/src/common/hoc';

import { Button, PageFragment, RootPageFragment } from '~/test/jest/utils';

jest.mock('~/src/features/Perfection/components/PerfectionTrafficLights/PerfectionTrafficLights.container.js');

export class OebsTab extends RootPageFragment {
    get loadMoreBtn() {
        return this.query(Button, '.Oebs-LoadMoreButton');
    }

    get tableBody() {
        return this.query(PageFragment, '.Oebs-TableBody');
    }

    row(row: number) {
        return this.tableBody?.container.children[row];
    }

    columnInRow(row: number, column: number) {
        return this.row(row)?.children[column];
    }
}

export function render(storeData: Record<string, unknown>, options?: Partial<StoreOptions>): OebsTab {
    const store = configureStore({
        initialState: storeData,
        fetcherOptions: {
            fetch: () => Promise.resolve({}),
        },
        sagaContextExtension: {
            api: {
                oebs: { requestOebsAgreements: () => Promise.resolve({}) },
            },
        },
        ...options,
    });

    const abcContextMock = {
        configs: {
            hosts: {
                centerClient: { protocol: 'https:', hostname: 'center.y-t.ru' },
                staff: { protocol: 'https:', hostname: 'staff.y-t.ru' },
            },
        },
        user: {} as User,
    };

    const OebsTabTable = withContext(withRedux(OebsTabConnected, store), abcContextMock);

    return new OebsTab(libRender(
        <OebsTabTable />,
    ));
}
