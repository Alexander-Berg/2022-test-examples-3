import { boundMethod } from 'autobind-decorator';
import identity from 'lodash/identity';
import React, { Component } from 'react';

import IconControl from 'client/components/icon-control';
import ProblemTestSelectCorrectness from 'client/components/problem-test/__select-correctness';
import { Props } from 'client/components/problem-test/__variants/types';
import Table, { ColumnProps } from 'client/components/table';
import block from 'client/utils/cn';
import i18n from 'client/utils/i18n';

import 'client/components/problem-test/__variants/problem-test__variants.css';

const b = block('problem-test');

class ProblemTestVariants extends Component<Props> {
    public render() {
        const { variants } = this.props;

        if (variants.length === 0) {
            return null;
        }

        return (
            <div className={b('variants')}>
                <Table
                    columns={this.getVariantColumns()}
                    data={variants}
                    preview
                    withFallback={false}
                />
            </div>
        );
    }

    private handleDeletionOfVariant(idx: number) {
        return () => this.props.onRemove(idx);
    }

    private handleVariantCorrectnessChange(idx: number) {
        return (value: boolean) => this.props.onChange(idx, value);
    }

    private renderSelectCorrectnessControl(answer: Props['answer']) {
        const { disabled } = this.props;

        return (_variant: string, idx: number) => (
            <ProblemTestSelectCorrectness
                value={answer.includes(idx + 1)}
                disabled={disabled}
                onChange={this.handleVariantCorrectnessChange(idx + 1)}
            />
        );
    }

    @boundMethod
    private renderVariantRemover(_variant: string, idx: number) {
        const { disabled } = this.props;

        if (disabled) {
            return null;
        }

        return (
            <IconControl
                size="m"
                type="close-16"
                cls={b('variants-remover-icon')}
                onClick={this.handleDeletionOfVariant(idx + 1)}
            />
        );
    }

    private getVariantColumns() {
        const { answer } = this.props;

        return [
            {
                title: i18n.text({ keyset: 'common', key: 'answer' }),
                render: identity,
            },
            {
                title: i18n.text({ keyset: 'common', key: 'is-correct' }),
                className: b('variants-control'),
                render: this.renderSelectCorrectnessControl(answer),
            },
            {
                className: b('variants-remover'),
                render: this.renderVariantRemover,
            },
        ] as Array<ColumnProps<string>>;
    }
}

export default ProblemTestVariants;
