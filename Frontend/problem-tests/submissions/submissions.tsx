import invert from 'lodash/invert';
import React, { useEffect, FC } from 'react';

import { ISubmission, VerdictShortName } from 'common/types/problem';
import { reportsUrl } from 'common/urls/client';

import Button from 'client/components/button';
import Link from 'client/components/link';
import { Props } from 'client/components/problem-tests/submissions/types';
import Table from 'client/components/table';
import block from 'client/utils/cn';
import { getHumanReadableShortDate } from 'client/utils/date';
import { formatDataSize, formatUsedTime } from 'client/utils/helpers';
import i18n from 'client/utils/i18n';

import 'client/components/problem-tests/submissions/submissions.css';

const b = block('submissions');

const Submissions: FC<Props> = ({
    className,
    submissions = [],
    problemId,
    rejudgeSubmission,
    rejudgeSubmissionStarted,
    clearSubmissionStatus,
    lastSubmissionStatus,
    readonly,
}) => {
    useEffect(() => {
        return () => {
            clearSubmissionStatus({ problemId });
        };
    }, []); //eslint-disable-line react-hooks/exhaustive-deps

    const longVerdicts = invert(VerdictShortName);

    const getTableColumns = () => {
        return [
            {
                title: i18n.text({ keyset: 'problem-settings', key: 'submission-id' }),
                render: ({ id }: ISubmission) => (
                    <Link
                        isExternal
                        type="default"
                        to={reportsUrl.runReportStandalone.build({ id })}
                    >
                        {id}
                    </Link>
                ),
                className: b('id'),
            },
            {
                title: i18n.text({ keyset: 'problem-settings', key: 'submission-filename' }),
                render: ({ fileName }: ISubmission) => <span title={fileName}>{fileName}</span>,
                className: b('filename'),
            },
            {
                title: i18n.text({ keyset: 'problem-settings', key: 'submission-user' }),
                render: ({ user }: ISubmission) => user,
                className: b('user'),
            },
            {
                title: i18n.text({ keyset: 'problem-settings', key: 'submission-time' }),
                render: ({ time }: ISubmission) => getHumanReadableShortDate(time),
                className: b('time'),
            },
            {
                title: i18n.text({ keyset: 'problem-settings', key: 'submission-compiler' }),
                render: ({ compilerId }: ISubmission) => compilerId,
                className: b('compiler'),
            },
            {
                title: i18n.text({ keyset: 'problem-settings', key: 'submission-verdict' }),
                render: ({ verdict }: ISubmission) => longVerdicts[verdict],
                className: b('verdict'),
            },
            {
                title: i18n.text({ keyset: 'problem-settings', key: 'submission-executionTime' }),
                render: ({ usedTimeMillis }: ISubmission) => {
                    return usedTimeMillis === null
                        ? usedTimeMillis
                        : formatUsedTime(usedTimeMillis);
                },
                className: b('execution-time'),
            },
            {
                title: i18n.text({ keyset: 'problem-settings', key: 'submission-memory' }),
                render: ({ usedMemoryBytes }: ISubmission) => {
                    return usedMemoryBytes === null
                        ? usedMemoryBytes
                        : formatDataSize(usedMemoryBytes);
                },
                className: b('memory'),
            },
            {
                render: (submission: ISubmission) => {
                    if (readonly) {
                        return null;
                    }

                    return (
                        <Button
                            size="s"
                            theme="normal"
                            disabled={rejudgeSubmissionStarted}
                            onClick={onRejudgeSubmissionClick(submission)}
                        >
                            {i18n.text({ keyset: 'problem-settings', key: 'rejudge-submission' })}
                        </Button>
                    );
                },
                className: b('rejudge'),
            },
        ];
    };

    const onRejudgeSubmissionClick = (submission: ISubmission) => {
        return () => {
            const { id: submissionId } = submission;

            rejudgeSubmission({ problemId, submissionId });
        };
    };

    const renderTable = () => {
        return (
            <Table
                data={submissions}
                className={b('table')}
                columns={getTableColumns()}
                hasFixedLayout
                fallbackText={i18n.text({ keyset: 'problem-settings', key: 'no-submissions' })}
            />
        );
    };

    return (
        <section className={b({}, [className])}>
            <h2 className={b('title')}>
                {i18n.text({ keyset: 'problem-settings', key: 'submissions' })}
            </h2>
            {lastSubmissionStatus && (
                <div className={b('submission-status')}>
                    {i18n.text({
                        keyset: 'problem-settings',
                        key: 'last-submission-status',
                        params: {
                            submissionStatus: lastSubmissionStatus,
                        },
                    })}
                </div>
            )}
            <div className={b('list')}>{renderTable()}</div>
        </section>
    );
};

export default Submissions;
