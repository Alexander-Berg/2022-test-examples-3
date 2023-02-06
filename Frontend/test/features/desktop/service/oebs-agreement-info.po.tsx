import React from 'react';
import { render as libRender } from '@testing-library/react';

import OebsAgreementInfo, { OebsAgreement } from '~/src/features/Service/components/OebsAgreementInfo/OebsAgreementInfo';

import { withContext } from '~/src/common/hoc';

import { PageFragment, RootPageFragment } from '~/test/jest/utils';
import { User } from '~/src/common/context/types';

export class OebsAgreementInfoPage extends RootPageFragment {
    get infoMessage() {
        return this.query(PageFragment, '.Message_type_info');
    }

    get link() {
        return this.get(PageFragment, '.Message_type_info .Link').container.innerHTML;
    }

    get url() {
        return this.get(PageFragment, '.Message_type_info .Link').container.getAttribute('href');
    }
}

export function render(oebsAgreement: OebsAgreement): OebsAgreementInfoPage {
    const OebsAgreementInfoConnected = withContext(OebsAgreementInfo, {
        configs: {
            hosts: {
                tracker: { protocol: 'https:', hostname: 'tracker.mock' },
            }
        },
        user: {} as User,
    });

    return new OebsAgreementInfoPage(libRender(
        <OebsAgreementInfoConnected oebsAgreement={oebsAgreement} />,
    ));
}
