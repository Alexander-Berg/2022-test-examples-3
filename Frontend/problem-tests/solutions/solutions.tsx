import React, { Fragment } from 'react';

import Submissions from 'client/components/problem-tests//submissions';
import AuthorSolutions from 'client/components/problem-tests/author-solutions';
import { Props } from 'client/components/problem-tests/solutions/types';
import Spin from 'client/components/spin';
import block from 'client/utils/cn';

import 'client/components/problem-tests/solutions/solutions.css';

const b = block('solutions');

const Solutions = ({ problemId, settings, className, readonly }: Props) => {
    const getSpinner = () => {
        return (
            <div className={b('loader')}>
                <Spin view="default" size="l" progress />
            </div>
        );
    };

    return (
        <section className={b({}, [className])}>
            {settings ? (
                <Fragment>
                    <AuthorSolutions
                        className={b('section')}
                        solutions={settings.solutions}
                        problemId={problemId}
                        readonly={readonly}
                    />
                    <Submissions
                        className={b('section')}
                        submissions={settings.submissions}
                        problemId={problemId}
                        readonly={readonly}
                    />
                </Fragment>
            ) : (
                getSpinner()
            )}
        </section>
    );
};

export default Solutions;
