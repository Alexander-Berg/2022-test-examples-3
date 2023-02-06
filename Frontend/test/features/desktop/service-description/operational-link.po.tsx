import React from 'react';
import { render as libRender } from '@testing-library/react';
import userEvent from '@testing-library/user-event';

import {
    ServiceContactsOperationalLink,
    ServiceContactsOperationalLinkProps,
} from '~/src/features/ServiceDescription/components/ServiceContacts/OperationalLink/ServiceContacts-OperationalLink';

import { PageFragment, RootPageFragment } from '~/test/jest/utils';

export class UrlButtonFragment extends PageFragment {
    click() {
        userEvent.click(this.link.container);
    }

    get content() {
        return this.get(PageFragment, '.Button2-Text').container.innerHTML;
    }

    get link() {
        return this.get(PageFragment, '.Button2');
    }

    get opensInNewWindow() {
        return this.link.container.getAttribute('target') === '_blank';
    }

    get url() {
        return this.link.container.getAttribute('href');
    }
}

export class OperationalLinkRootFragment extends RootPageFragment {
    get buttons() {
        return this.getAll(UrlButtonFragment, '.ServiceContacts-UrlButton');
    }

    get iframe() {
        return this.query(PageFragment, '.ServiceContacts-OperationalLinkIframe');
    }

    get spinner() {
        return this.query(PageFragment, '.Spin2');
    }
}

export function render(props: ServiceContactsOperationalLinkProps): OperationalLinkRootFragment {
    return new OperationalLinkRootFragment(libRender(
        <ServiceContactsOperationalLink {...props} />,
    ));
}
