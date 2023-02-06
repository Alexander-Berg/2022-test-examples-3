import isUndefined from 'lodash/isUndefined';
import React, { useEffect, Fragment } from 'react';

import { hasWriteAccessToEntity } from 'common/utils/helpers/has-access';
import { isSimpleProblem } from 'common/utils/problem';

import Button from 'client/components/button';
import CreateTest from 'client/components/create-test';
import Page from 'client/components/page';
import ProblemGenerators from 'client/components/problem-generators';
import Solutions from 'client/components/problem-tests/solutions';
import { Props } from 'client/components/problem-tests/types';
import ProblemTestsets from 'client/components/problem-testsets';
import ProblemValidators from 'client/components/problem-validators';

import block from 'client/utils/cn';
import i18n from 'client/utils/i18n';

import 'client/components/problem-tests/problem-tests.css';

const b = block('problem-tests');

const ProblemTests = ({ problemId, settings, fetchStarted, getSettings, permission }: Props) => {
    useEffect(() => {
        if (isUndefined(settings) && !fetchStarted) {
            getSettings({ problemId });
        }
    }, []); // eslint-disable-line react-hooks/exhaustive-deps

    const renderSolutions = () => {
        if (settings && isSimpleProblem(settings.type)) {
            return null;
        }

        return <Solutions className={b('section')} problemId={problemId} readonly={isReadonly} />;
    };

    const isReadonly = !hasWriteAccessToEntity(permission);

    return (
        <Fragment>
            <header className={b('header')}>
                <Page.Title>{i18n.text({ keyset: 'common', key: 'problem-tests' })}</Page.Title>
                {!isReadonly && (
                    <CreateTest problemId={problemId}>
                        {(openModal) => (
                            <Button theme="normal" size="m" onClick={openModal}>
                                {i18n.text({ keyset: 'problem-tests', key: 'create-test' })}
                            </Button>
                        )}
                    </CreateTest>
                )}
            </header>
            {renderSolutions()}
            <ProblemGenerators
                className={b('section')}
                problemId={problemId}
                readonly={isReadonly}
            />
            <ProblemValidators
                className={b('section')}
                problemId={problemId}
                readonly={isReadonly}
            />
            <ProblemTestsets className={b('section')} problemId={problemId} readonly={isReadonly} />
        </Fragment>
    );
};

export default ProblemTests;
