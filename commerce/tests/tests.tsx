import { cn } from '@bem-react/classname';
import React, { Component } from 'react';
import { Button, Link, Tooltip } from 'lego-on-react';

import './tests.css';

import 'lego-on-react/src/components/button/button.css';
import 'lego-on-react/src/components/link/link.css';
import 'lego-on-react/src/components/tooltip/tooltip.css';
import Dictionary from '../../../common/types/dictionary';
import IExam from '../../../common/types/exam';

import { ILockedExam } from './index';

const b = cn('Tests');

interface ITestsProps {
    exams: IExam[],
    api: string,
    lockedExams: Dictionary<ILockedExam>,
    visibleLockName: string | null,
    showTooltip(lockName: string): void,
    hideTooltip(): void
}

class Tests extends Component<ITestsProps> {
    private locks: Dictionary<HTMLSpanElement> = {};

    private onMouseOver = (event: React.MouseEvent<HTMLElement>): void => {
        const { showTooltip } = this.props;
        const target = event.target as HTMLElement;

        showTooltip(target.dataset.slug!);
    };

    private onMouseLeave = (): void => {
        const { hideTooltip } = this.props;

        hideTooltip();
    };

    render() {
        const { exams, api, lockedExams, visibleLockName } = this.props;

        return (
            <div className={b()}>
                {exams.map(exam => {
                    const { id, slug } = exam;
                    const lockedExam = lockedExams[slug];

                    return (
                        <div key={id} className={b('Test')}>
                            <div className={b('Edit')}>
                                {
                                    lockedExam &&
                                    <div className={b('Lock-wrapper')}>
                                        <span
                                            className={b('Lock')}
                                            title={lockedExam.login}
                                            ref={node => { this.locks[slug] = node!; }}
                                            onMouseOver={this.onMouseOver}
                                            onMouseLeave={this.onMouseLeave}
                                            data-slug={slug}
                                            />
                                        <Tooltip
                                            cls={b('Tooltip')}
                                            theme="normal"
                                            anchor={this.locks[slug]}
                                            to="top"
                                            visible={visibleLockName === slug}
                                            >
                                            <span className={b('TooltipLogin')}>
                                                {lockedExams[slug].login}
                                            </span>
                                        </Tooltip>
                                    </div>
                                }
                                <Link
                                    theme="normal"
                                    url={`/admin/${slug}`} cls={b('Name')}
                                    text={`/${slug}`}
                                    />
                            </div>
                            <Button
                                theme="normal"
                                title={exam.title}
                                size="m"
                                type="link"
                                text="Скачать"
                                url={`${api}/admin/downloadXLSX/${exam.id}`}
                                />
                        </div>
                    );
                })}
            </div>
        );
    }
}

export default Tests;
