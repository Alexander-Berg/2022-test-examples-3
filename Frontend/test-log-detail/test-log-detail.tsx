import React, { useState, FC, useMemo } from 'react';

import block from 'client/utils/cn';
import { formatDataSize, formatUsedTime } from 'client/utils/helpers';
import i18n from 'client/utils/i18n';

import { TextAnswerViewMode } from 'client/components/hex-area/types';
import { scoreKeyByTypeDict } from 'client/components/submission/constants';
import {
    ITabState,
    Props,
    ViewTabData,
    ViewTabType,
} from 'client/components/submission/test-log-detail/types';
import { VerdictShortName } from 'common/types/problem';
import { ISubmissionTest, SubmissionScoreType } from 'common/types/submission';

import Accordion from 'client/components/accordion';
import ProblemTextAnswerHexView from 'client/components/hex-area/__hex-view';
import ProblemTextAnswerHiddenView from 'client/components/hex-area/__hidden-view';
import Icon from 'client/components/icon';
import SubmissionPublicLog from 'client/components/submission/public-log';
import ViewTab from 'client/components/submission/view-tab';
import IconControl from 'client/components/icon-control';
import { submissionUrls } from 'common/urls/api';

const b = block('submission-tests');

const TestLogDetail: FC<Props> = ({ submissionId, test, copyToClipboard }) => {
    const [selectedTab, changeSelectedTab] = useState<ITabState>({
        input: TextAnswerViewMode.RAW,
        output: TextAnswerViewMode.RAW,
        error: TextAnswerViewMode.RAW,
        message: TextAnswerViewMode.RAW,
        answer: TextAnswerViewMode.RAW,
    });

    function handleCopy(value: string) {
        return () => copyToClipboard({ value });
    }

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
        if (!score || score.scoreType === SubmissionScoreType.NONE) {
            return '—';
        }

        const scoreKey = scoreKeyByTypeDict[score.scoreType];
        const scoreValue = (scoreKey && score[scoreKey]?.toString()) ?? '';

        return scoreValue;
    }

    function renderResources({ runningTime, memoryUsed }: ISubmissionTest) {
        return `${formatUsedTime(runningTime)} / ${formatDataSize(memoryUsed)}`;
    }

    function getTabContent(type: ViewTabType, value: string) {
        switch (selectedTab[type]) {
            case TextAnswerViewMode.RAW:
                return <pre className={b('raw')}>{value}</pre>;
            case TextAnswerViewMode.HIDDEN:
                return <ProblemTextAnswerHiddenView className={b('hidden-chars')} value={value} />;
            case TextAnswerViewMode.HEX:
                return <ProblemTextAnswerHexView className={b('hex')} value={value} />;
        }
    }

    function handleChangeViewTab(type: ViewTabType) {
        return (value: TextAnswerViewMode) => {
            changeSelectedTab({
                ...selectedTab,
                [type]: value,
            });
        };
    }

    const items = useMemo(() => {
        const tabsData: ViewTabData[] = [
            {
                title: i18n.text({ keyset: 'submission', key: 'input' }),
                tab: 'input',
                value: test.input,
                downloadUrl: submissionUrls.submissionDownloadInput.build(
                    {
                        submissionId: submissionId.toString(),
                    },
                    {
                        testName: encodeURI(test.testName),
                    },
                ),
            },
            {
                title: i18n.text({ keyset: 'submission', key: 'output' }),
                tab: 'output',
                value: test.output,
                downloadUrl: submissionUrls.submissionDownloadOutput.build(
                    {
                        submissionId: submissionId.toString(),
                    },
                    {
                        testName: encodeURI(test.testName),
                    },
                ),
            },
            {
                title: i18n.text({ keyset: 'submission', key: 'error' }),
                tab: 'error',
                value: test.error,
            },
            {
                title: i18n.text({ keyset: 'submission', key: 'answer' }),
                tab: 'answer',
                value: test.answer,
                downloadUrl: submissionUrls.submissionDownloadAnswer.build(
                    {
                        submissionId: submissionId.toString(),
                    },
                    {
                        testName: encodeURI(test.testName),
                    },
                ),
            },
            {
                title: i18n.text({ keyset: 'submission', key: 'checker-message' }),
                tab: 'message',
                value: test.message,
            },
        ];

        return tabsData.filter((item) => item.value);
    }, [test, submissionId]);

    return (
        <div className={b({})}>
            <dl className={b('common')}>
                <dt className={b('term')}>
                    {i18n.text({ keyset: 'submission', key: 'verdict' })}:
                </dt>
                <dd className={b('value')}>{renderVerdict(test)}</dd>

                <dt className={b('term')}>{i18n.text({ keyset: 'submission', key: 'score' })}:</dt>
                <dd className={b('value')}>{renderScore(test)}</dd>

                <dt className={b('term')}>
                    {i18n.text({ keyset: 'submission', key: 'resources' })}:
                </dt>
                <dd className={b('value')}>{renderResources(test)}</dd>
            </dl>

            {items.map((item) => (
                <div className={b('item')} key={item.tab}>
                    <div className={b('title')}>
                        {item.title}
                        {item.downloadUrl && (
                            <IconControl
                                cls={b('control')}
                                type="download-16"
                                url={item.downloadUrl}
                                external
                            />
                        )}
                        <IconControl
                            hint={i18n.text({ keyset: 'common', key: 'copy-to-clipboard' })}
                            cls={b('control')}
                            type="copy-16"
                            size="m"
                            onClick={handleCopy(item.value)}
                        />
                    </div>
                    <ViewTab
                        value={selectedTab[item.tab]}
                        onChange={handleChangeViewTab(item.tab)}
                    />
                    <div className={b('field')}>{getTabContent(item.tab, item.value)}</div>
                </div>
            ))}

            <Accordion
                className={b('log')}
                title={i18n.text({ keyset: 'submission', key: 'compile-log' })}
            >
                <SubmissionPublicLog publicLog={test.publicLog} />
            </Accordion>
        </div>
    );
};

export default TestLogDetail;
