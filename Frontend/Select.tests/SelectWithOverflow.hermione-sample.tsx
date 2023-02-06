import React from 'react';
import { render } from 'react-dom';
import { configureRootTheme } from '../../Theme';
import { theme } from '../../Theme/presets/default';
import { Select } from '../desktop/bundle';
import { Hermione } from '../../internal/components/Hermione/Hermione';

configureRootTheme({ theme });

// Generates the number of options needed for overflow in the select popup
const items: number[] = [];
for (let i = 0; i < 40; i++) {
    items.push(i);
}

const SelectWithOverflowHermioneSample = () => (
    <Hermione
        className={'Overflow'}
        style={{
            position: 'absolute',
            top: 0,
            bottom: 0,
            display: 'flex',
            alignItems: 'flex-end',
        }}
    >
        <Select
            options={items.map((i) => ({
                value: i,
                content: `option ${i}`,
            }))}
            size="s"
            view="default"
        />
    </Hermione>
);

render(<SelectWithOverflowHermioneSample />, document.getElementById('root'));
