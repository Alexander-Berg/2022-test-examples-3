import { boundMethod } from 'autobind-decorator';
import React, { ChangeEvent, Component } from 'react';

import { Props } from 'client/components/problem-test/__select-correctness/types';
import RadioButton from 'client/components/radio-button';
import i18n from 'client/utils/i18n';

class ProblemTestSelectCorrectness extends Component<Props> {
    public render() {
        const { value, disabled } = this.props;
        const radioValue = value ? '1' : '';

        return (
            <RadioButton
                view="default"
                size="m"
                value={radioValue}
                disabled={disabled}
                onChange={this.handleVariantCorrectnessChange}
                options={[
                    {
                        value: '1',
                        children: i18n.text({ keyset: 'common', key: 'yes' }),
                    },
                    {
                        value: '',
                        children: i18n.text({ keyset: 'common', key: 'no' }),
                    },
                ]}
            />
        );
    }

    @boundMethod
    private handleVariantCorrectnessChange(event: ChangeEvent<HTMLInputElement>) {
        this.props.onChange(event.currentTarget.value === '1');
    }
}

export default ProblemTestSelectCorrectness;
