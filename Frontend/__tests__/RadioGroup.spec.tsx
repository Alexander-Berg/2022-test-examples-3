import * as React from 'react';
import { shallow } from 'enzyme';
import { RadioGroup } from '../RadioGroup';

const colors = ['green', 'red', 'blue', 'black', 'yellow', 'pink'];
const colorItems = colors.map(clr => ({ name: 'color', type: 'color', value: clr }));

it('renders without crashing', () => {
    shallow(
        <RadioGroup name="color" items={colorItems} />
    );
});
