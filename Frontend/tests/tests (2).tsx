import React, { Fragment } from 'react';
import Helmet from 'react-helmet';

import ProblemTests from 'client/components/problem-tests';
import { Props } from 'client/pages/problems/tests/types';
import i18n from 'client/utils/i18n';

const ProblemTestsPage = ({ match, getTitle }: Props) => {
    const { id } = match.params;

    if (!id) {
        return null;
    }

    return (
        <Fragment>
            <Helmet>
                <title>{getTitle(i18n.text({ keyset: 'common', key: 'problem-tests' }))}</title>
            </Helmet>
            <ProblemTests problemId={id} />
        </Fragment>
    );
};

export default ProblemTestsPage;
