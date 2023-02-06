import { boundMethod } from 'autobind-decorator';
import memoize from 'lodash/memoize';
import React, { Component } from 'react';

import { IProblemTestsetBriefInfo } from 'common/types/problem-test';

import Button from 'client/components/button';
import ProblemTestset from 'client/components/problem-testsets/problem-testset';
import { Props } from 'client/components/problem-testsets/types';
import SectionLoader from 'client/components/section-loader';
import block from 'client/utils/cn';
import i18n from 'client/utils/i18n';

import 'client/components/problem-testsets/problem-testsets.css';

const b = block('problem-testsets');

class ProblemTestsets extends Component<Props> {
    private handleRemove = memoize((testsetId: number) => () => {
        const { problemId, removeTestset } = this.props;

        removeTestset({ problemId, testsetId });
    });

    private handleUpdateTests = memoize((testsetId: number) => () => {
        const { problemId, updateTestset } = this.props;

        updateTestset({ problemId, testsetId, data: {} });
    });

    private handleChangeTest = memoize(
        (testsetId: number) => (fieldName: string, value: string = '') => {
            const { problemId, updateTestset } = this.props;

            updateTestset({ problemId, testsetId, data: { [fieldName]: value } });
        },
    );

    public componentDidMount() {
        const { problemId, fetchTestsets } = this.props;

        fetchTestsets({ problemId });
    }

    public render() {
        const { readonly, className, fetchStarted = true, addTestsetStarted } = this.props;

        return (
            <section className={b({}, [className])}>
                <h2 className={b('title')}>
                    {i18n.text({ keyset: 'problem-tests', key: 'testsets' })}
                </h2>
                {!readonly && (
                    <Button
                        theme="normal"
                        size="m"
                        className={b('control')}
                        onClick={this.handleAddTestset}
                        disabled={fetchStarted || addTestsetStarted}
                    >
                        {i18n.text({ keyset: 'problem-tests', key: 'testsets__add-testset' })}
                    </Button>
                )}
                {this.renderAddExampleButton()}
                <div className={b('testsets')}>{this.renderTestsets()}</div>
            </section>
        );
    }

    private renderAddExampleButton() {
        const { readonly, addSampleStarted, fetchStarted = true } = this.props;

        if (this.hasExample || readonly) {
            return null;
        }

        return (
            <Button
                theme="normal"
                size="m"
                className={b('control')}
                onClick={this.handleAddSample}
                disabled={addSampleStarted || fetchStarted}
            >
                {i18n.text({ keyset: 'problem-tests', key: 'testsets__add-samples' })}
            </Button>
        );
    }

    private renderTestsets() {
        const { fetchStarted } = this.props;

        if (fetchStarted) {
            return <SectionLoader />;
        }

        return <ul className={b('testsets-list')}>{this.renderTestsetsList()}</ul>;
    }

    private renderTestsetsList() {
        const { problemId, testsets, readonly } = this.props;

        return testsets.map((testset: IProblemTestsetBriefInfo) => {
            const { id, name } = testset;

            return (
                <ProblemTestset
                    key={name}
                    problemId={problemId}
                    testsetId={id}
                    onChange={this.handleChangeTest(id)}
                    onRemove={this.handleRemove(id)}
                    updateTests={this.handleUpdateTests(id)}
                    readonly={readonly}
                />
            );
        });
    }

    @boundMethod
    private handleAddTestset() {
        const { problemId, addTestset } = this.props;

        addTestset({ problemId });
    }

    @boundMethod
    private handleAddSample() {
        const { problemId, addSample } = this.props;

        addSample({ problemId });
    }

    private get hasExample() {
        const { testsets = [] } = this.props;

        if (testsets.length === 0) {
            return false;
        }

        return testsets[0].sample;
    }
}

export default ProblemTestsets;
