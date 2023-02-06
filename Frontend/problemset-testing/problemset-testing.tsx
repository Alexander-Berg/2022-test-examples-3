import { boundMethod } from 'autobind-decorator';
import React, { Component } from 'react';

import i18n from 'client/utils/i18n';
import {
    ICustomLimit,
    IFileSettings,
    ILimits,
    IProblem,
    IPropertyLimits,
} from 'common/types/problem';
import { IProblemTestset } from 'common/types/problem-test';
import { problemsUrls } from 'common/urls/client';

import { Props } from 'client/components/contest-problemset/problemset-testing/types';
import LegoLink from 'client/components/lego-link';
import { Memory } from 'client/components/suffix-value';
import Table from 'client/components/table';
import block from 'client/utils/cn';

import 'client/components/contest-problemset/problemset-testing/problemset-testing.css';

const b = block('problemset-testing');

class ProblemsetTesting extends Component<Props> {
    private columns = [
        {
            className: b('table-cell', { 'aligned-top': true, 'centered-arrow': true }),
            render: (problem: IProblem) => problem.title,
        },
        {
            title: i18n.text({ keyset: 'common', key: 'name' }),
            className: b('table-cell', { 'aligned-top': true, 'name': true }),
            render: ({ id, name, type }: IProblem) => (
                <div className={b('problem')}>
                    <LegoLink
                        theme="normal"
                        href={problemsUrls.settings.build({ id })}
                        className={b('problem-name')}
                    >
                        {name}
                    </LegoLink>
                    <span className={b('problem-type', { muted: true })}>{type}</span>
                </div>
            ),
        },
        {
            title: i18n.text({ keyset: 'contest-problemset', key: 'tests-set' }),
            className: b('table-cell', { 'aligned-top': true }),
            render: ({ testSets }: IProblem) => (
                <ul className={b('list')}>{this.renderTestSets(testSets)}</ul>
            ),
        },
        {
            title: i18n.text({ keyset: 'contest-settings', key: 'checker' }),
            className: b('table-cell', { 'aligned-top': true }),
            render: (problem: IProblem) => {
                const checkers = problem.checkerSettings.details.checkerFiles || [];

                return <ul className={b('list')}>{this.renderChecker(checkers)}</ul>;
            },
        },
        {
            title: i18n.text({ keyset: 'contest-settings', key: 'checker__type' }),
            className: b('table-cell', { 'aligned-top': true }),
            render: (problem: IProblem) => {
                const checkerType = problem.checkerSettings.type;

                return <span className={b('checker-type')}>{checkerType}</span>;
            },
        },
        {
            title: i18n.text({ keyset: 'contest-settings', key: 'memory_mb' }),
            className: b('table-cell', { 'aligned-top': true }),
            render: ({ runtimeLimit }: IProblem) => this.renderLimit(runtimeLimit, 'memoryLimit'),
        },
        {
            title: i18n.text({ keyset: 'contest-settings', key: 'time_mc' }),
            className: b('table-cell', { 'aligned-top': true }),
            render: ({ runtimeLimit }: IProblem) => this.renderLimit(runtimeLimit, 'timeLimit'),
        },
        {
            title: i18n.text({ keyset: 'common', key: 'files' }),
            className: b('table-cell', { 'aligned-top': true }),
            render: ({ fileSettings }: IProblem) => this.renderFileset(fileSettings),
        },
    ];
    public render() {
        const { problemset } = this.props;
        return (
            <div className={b()}>
                <Table columns={this.columns} data={problemset.problems} />
            </div>
        );
    }
    private renderTestSets(tests: IProblemTestset[]) {
        if (!tests) {
            return null;
        }

        if (tests.length === 0) {
            return <span>{i18n.text({ keyset: 'common', key: 'no-files' })}</span>;
        }
        return tests.map(({ name, tests }: IProblemTestset) => (
            <li className={b('list-item', { bordered: true, aligned: true })} key={name}>
                <span>{name}</span>
                <span>{tests.length}</span>
            </li>
        ));
    }
    private renderChecker(checkers: string[]) {
        if (!checkers) {
            return null;
        }

        if (checkers.length === 0) {
            return <span>{i18n.text({ keyset: 'common', key: 'no-files' })}</span>;
        }
        return checkers.map((checker: string) => {
            return (
                <li
                    className={b('list-item', {
                        bordered: true,
                        checker: true,
                    })}
                    key={checker}
                >
                    {checker}
                </li>
            );
        });
    }
    private renderFileset({
        inputFileName,
        outputFileName,
        allowReadStdin,
        allowWriteStdout,
    }: IFileSettings) {
        const allowStdinKey = allowReadStdin ? 'yes' : 'no';
        const allowStdoutKey = allowWriteStdout ? 'yes' : 'no';

        return (
            <ul className={b('list')}>
                <li className={b('list-item', { bordered: true })}>{inputFileName}</li>
                <li className={b('list-item', { bordered: true })}>{outputFileName}</li>
                <li className={b('list-item', { bordered: true })}>
                    {i18n.text({ keyset: 'common', key: allowStdinKey })}/
                    {i18n.text({ keyset: 'common', key: allowStdoutKey })}
                </li>
            </ul>
        );
    }
    @boundMethod
    private renderLimit(limit: IPropertyLimits, property: keyof ILimits) {
        const { custom, common } = limit;

        return (
            <ul className={b('list')}>
                <li className={b('list-item', { bordered: true })}>
                    {this.renderLimitValue(common[property], property)}
                </li>
                {this.renderCompilerLimits(custom, property)}
            </ul>
        );
    }

    private renderLimitValue(value: number, property: keyof ILimits) {
        return property === 'memoryLimit' ? (
            <Memory value={value}>
                {(representValue, prefix = '') => (
                    <span>
                        {representValue} {prefix}
                    </span>
                )}
            </Memory>
        ) : (
            value
        );
    }

    private renderCompilerLimits(limits: ICustomLimit[] = [], property: keyof ILimits) {
        if (limits.length === 0) {
            return null;
        }

        return (
            <li className={b('list-item', { bordered: true })}>
                <ul className={b('list')}>
                    {limits.map((limit: ICustomLimit) => {
                        const key = limit.compilers.join(', ');

                        return (
                            <li key={key} className={b('list-item', { aligned: true })}>
                                <span>{key}</span>
                                <span>
                                    {this.renderLimitValue(limit.limit[property], property)}
                                </span>
                            </li>
                        );
                    })}
                </ul>
            </li>
        );
    }
}

export default ProblemsetTesting;
