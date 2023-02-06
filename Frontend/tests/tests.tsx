import React, { useMemo, useState, ChangeEvent, FC } from 'react';
import { HashLink } from 'react-router-hash-link';

import block from 'client/utils/cn';
import { formatDataSize, formatUsedTime } from 'client/utils/helpers';
import i18n from 'client/utils/i18n';

import { scoreKeyByTypeDict } from 'client/components/submission/constants';
import { Props, TestsTab } from 'client/components/submission/tests/types';
import { VerdictShortName } from 'common/types/problem';
import { ISubmissionTest, SubmissionScoreType } from 'common/types/submission';

import Accordion from 'client/components/accordion';
import Button from 'client/components/button';
import Icon from 'client/components/icon';
import RadioButton from 'client/components/radio-button';
import Spin from 'client/components/spin';
import TestLogDetail from 'client/components/submission/test-log-detail';
import Table, { ColumnProps } from 'client/components/table';

import 'client/components/submission/tests/tests.css';

const b = block('submission-tests');

const link = block('link');

const Tests: FC<Props> = ({ submissionId, tests, isLoading, onShowTestDetail }) => {
    const failedTests = tests.filter(({ verdict }) => verdict !== VerdictShortName.OK);
    const [selectedTab, changeSelectedTab] = useState(TestsTab.FAILED);
    const [expandedTests, setExpandedTests] = useState(new Set());

    const hasFailedTests = failedTests.length > 0;
    const showFailedTest = selectedTab === TestsTab.FAILED && hasFailedTests;
    const selectedTests = showFailedTest ? failedTests : tests;

    function renderVerdict({ verdict }: ISubmissionTest) {
        const validityIcon =
            verdict === VerdictShortName.OK ? (
                <Icon glyph="type-check" size="s" className={b('validity-icon', { valid: true })} />
            ) : (
                <Icon type="red-circle-10" size="xs" className={b('validity-icon')} />
            );

        return (
            <>
                {validityIcon} {verdict}
            </>
        );
    }

    function renderScore({ score }: ISubmissionTest) {
        if (isEmptyScore(score)) {
            return '—';
        }

        const scoreKey = score && scoreKeyByTypeDict[score.scoreType];
        const scoreValue = (scoreKey && score && score[scoreKey]?.toString()) ?? '';

        return scoreValue;
    }

    function renderResources({ runningTime, memoryUsed }: ISubmissionTest) {
        return `${formatUsedTime(runningTime)} / ${formatDataSize(memoryUsed)}`;
    }

    function isEmptyScore(score: ISubmissionTest['score']) {
        return !score || score.scoreType === SubmissionScoreType.NONE;
    }

    function handleTabChange(event: ChangeEvent<HTMLInputElement>) {
        const value = event.target.value as TestsTab;
        changeSelectedTab(value);
    }

    function handleDetailToggle(hash: string) {
        return () => {
            const newSet = new Set(expandedTests);
            if (newSet.has(hash)) {
                newSet.delete(hash);
            } else {
                onShowTestDetail();
                newSet.add(hash);
            }

            setExpandedTests(newSet);
        };
    }

    function handleDetailToggleAll() {
        const newSet = new Set();

        if (!expandedTests.size) {
            selectedTests.forEach((test, index) => {
                const { testName } = test;
                const hash = `#${testName}-${index}`;
                newSet.add(hash);
            });
        }

        setExpandedTests(newSet);
    }

    function handleDetailShow(hash: string) {
        return () => {
            const newSet = new Set(expandedTests);
            newSet.add(hash);

            setExpandedTests(newSet);
            onShowTestDetail();
        };
    }

    const hasScoreColumn = useMemo(() => {
        return selectedTests.some((test) => !isEmptyScore(test.score));
    }, [selectedTests]);

    const columns = [
        {
            title: i18n.text({ keyset: 'submission', key: 'test-number' }),
            render: ({ testName }: ISubmissionTest, index?: number) => {
                const hash = `#${testName}-${index}`;
                return (
                    <HashLink
                        smooth
                        to={hash}
                        className={link({})}
                        onClick={handleDetailShow(hash)}
                    >
                        {testName}
                    </HashLink>
                );
            },
        },
        {
            title: i18n.text({ keyset: 'submission', key: 'verdict' }),
            render: renderVerdict,
        },
        hasScoreColumn && {
            title: i18n.text({ keyset: 'submission', key: 'score' }),
            render: renderScore,
        },
        {
            title: i18n.text({ keyset: 'submission', key: 'resources' }),
            render: renderResources,
        },
    ].filter(Boolean) as Array<ColumnProps<ISubmissionTest>>;

    const tabOptions = [
        {
            value: TestsTab.FAILED,
            children: (
                <>
                    {i18n.text({ keyset: 'submission', key: 'failed-tests' })}
                    <span className={b('tab-count')}>{failedTests.length}</span>
                </>
            ),
        },
        {
            value: TestsTab.ALL,
            children: (
                <>
                    {i18n.text({ keyset: 'submission', key: 'all-tests' })}
                    <span className={b('tab-count')}>{tests.length}</span>
                </>
            ),
        },
    ];

    if (isLoading) {
        return (
            <div className={b('loader')}>
                <Spin view="default" size="m" progress />
            </div>
        );
    }

    return (
        <div className={b({})}>
            {hasFailedTests && (
                <RadioButton
                    className={b('tabs')}
                    size="s"
                    view="default"
                    options={tabOptions}
                    value={selectedTab}
                    onChange={handleTabChange}
                />
            )}
            <Table className={b('list')} columns={columns} data={selectedTests} />
            <Button
                theme="normal"
                size="s"
                className={b('toggle-all-button')}
                onClick={handleDetailToggleAll}
            >
                {expandedTests.size
                    ? i18n.text({ keyset: 'submission', key: 'hide-all' })
                    : i18n.text({ keyset: 'submission', key: 'expand-all' })}
            </Button>
            <div className={b('details')}>
                {selectedTests.map((test, index) => {
                    const { testName } = test;
                    const currentHash = `#${testName}-${index}`;
                    const isActive = expandedTests.has(currentHash);
                    return (
                        <Accordion
                            key={index}
                            title={testName}
                            id={`${testName}-${index}`}
                            isOpen={isActive}
                            onClick={handleDetailToggle(currentHash)}
                        >
                            <TestLogDetail submissionId={submissionId} test={test} />
                        </Accordion>
                    );
                })}
            </div>
        </div>
    );
};

export default Tests;
