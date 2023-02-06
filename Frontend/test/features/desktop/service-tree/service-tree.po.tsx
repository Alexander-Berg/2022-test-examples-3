import React from 'react';
import { render as libRender } from '@testing-library/react';
import userEvent from '@testing-library/user-event';

import ServiceTree from '~/src/features/ServiceTree/ServiceTree';

import { configureStore } from '~/src/abc/react/redux/store';
import { withRedux } from '~/src/common/hoc';

import { PageFragment, RootPageFragment } from '~/test/jest/utils';

export class CutFragment extends PageFragment {
    click() {
        userEvent.click(this.get(PageFragment, '.Cut-Control button').container);
    }

    get isOpen() {
        return this.get(PageFragment, '.Cut-Control .Icon').container.classList.contains('Icon_direction_top');
    }
}

export class ErrorMessageFragment extends PageFragment {
    get message() {
        return this.container.innerHTML;
    }
}

export class OebsServiceFragment extends PageFragment {
    get name() {
        return this.get(PageFragment, '.OebsService-Name .Link').container.innerHTML;
    }

    get url() {
        return this.get(PageFragment, '.OebsService-Name .Link').container.getAttribute('href');
    }

    get cut() {
        return this.get(CutFragment, '.Cut');
    }
}

export class ServiceTreeRootFragment extends RootPageFragment {
    get spinner() {
        return this.query(PageFragment, '.Spin2');
    }

    get error() {
        return this.query(ErrorMessageFragment, '.Error-MessagePlace');
    }

    get services() {
        return this.getAll(OebsServiceFragment, '.OebsService');
    }
}

export function render(storeData: Record<string, unknown>): ServiceTreeRootFragment {
    const store = configureStore({
        initialState: storeData,
        fetcherOptions: {
            fetch: () => Promise.resolve(),
        },
    });

    const ServiceTreeConnected = withRedux(ServiceTree, store);

    return new ServiceTreeRootFragment(libRender(
        <ServiceTreeConnected />,
    ));
}
