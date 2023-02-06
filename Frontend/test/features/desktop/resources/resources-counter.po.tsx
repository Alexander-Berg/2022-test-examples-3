import React from 'react';
import { render as libRender } from '@testing-library/react';

import ResourcesCounter from '~/src/features/ResourcesCounter/ResourcesCounter';

import { configureStore } from '~/src/abc/react/redux/store';
import { withRedux } from '~/src/common/hoc';

import { PageFragment, RootPageFragment } from '~/test/jest/utils';
import { ResourcesCountsResponse } from '~/src/features/ResourcesCounter/redux/types/requests';

export class CardFeatureFragment extends PageFragment {
    get url() {
        return this.container.getAttribute('href');
    }

    get name() {
        return this.get(PageFragment, '.CloudCard-Feature-Name').container.textContent;
    }

    get value() {
        return this.get(PageFragment, '.CloudCard-Feature-Value').container.textContent;
    }
}

export class CardFragment extends PageFragment {
    get url() {
        return this.container.getAttribute('href');
    }

    get title() {
        return this.get(PageFragment, '.CloudCard-Title').container.textContent;
    }

    get features() {
        return this.getAll(CardFeatureFragment, '.CloudCard-Feature');
    }
}

export class ResourcesCounterRootFragment extends RootPageFragment {
    get spinner() {
        return this.query(PageFragment, '.Spin2');
    }

    get cards() {
        return this.getAll(CardFragment, '.CloudCard');
    }
}

export function render(
    storeData: Record<string, unknown>,
    counterResponse: Promise<ResourcesCountsResponse>,
): ResourcesCounterRootFragment {
    const store = configureStore({
        initialState: storeData,
        fetcherOptions: {
            fetch: () => Promise.resolve(),
        },
        sagaContextExtension: {
            api: {
                resourcesCounter: { requestResourcesCounter: () => counterResponse },
            },
        },
    });

    const ResourcesCounterConnected = withRedux(ResourcesCounter, store);

    return new ResourcesCounterRootFragment(libRender(
        <ResourcesCounterConnected />,
    ));
}
