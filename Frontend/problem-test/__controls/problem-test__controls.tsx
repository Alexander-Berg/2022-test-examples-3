import { boundMethod } from 'autobind-decorator';
import React, { ChangeEvent, Component, KeyboardEvent } from 'react';

import Button from 'client/components/button';
import { Props, State } from 'client/components/problem-test/__controls/types';
import Textinput from 'client/components/textinput';
import block from 'client/utils/cn';
import i18n from 'client/utils/i18n';

import 'client/components/problem-test/__controls/problem-test__controls.css';

const b = block('problem-test');

class ProblemTestControls extends Component<Props, State> {
    public readonly state: State = {
        variantText: '',
    };

    public render() {
        const { disabled } = this.props;
        const { variantText } = this.state;

        return (
            <div className={b('controls')}>
                <Textinput
                    theme="normal"
                    size="m"
                    className={b('controls-input')}
                    value={variantText}
                    disabled={disabled}
                    onChange={this.handleVariantTextChange}
                    onKeyDown={this.handleKeyDown}
                />
                <Button
                    theme="normal"
                    pin="clear-round"
                    size="m"
                    disabled={disabled}
                    onClick={this.handleAdditionOfVariant}
                >
                    {i18n.text({ keyset: 'problem-settings', key: 'add-answer-variant' })}
                </Button>
            </div>
        );
    }

    @boundMethod
    private handleKeyDown(event: KeyboardEvent) {
        if (event.key === 'Enter') {
            event.preventDefault();
            event.stopPropagation();
            this.handleAdditionOfVariant();
        }
    }

    @boundMethod
    private handleVariantTextChange(event: ChangeEvent<HTMLInputElement>) {
        this.setState({ variantText: event.target.value });
    }

    @boundMethod
    private handleAdditionOfVariant() {
        const { variantText } = this.state;
        const { onAdd } = this.props;

        if (variantText.trim().length === 0) {
            return;
        }

        onAdd(variantText);
        this.setState({ variantText: '' });
    }
}

export default ProblemTestControls;
