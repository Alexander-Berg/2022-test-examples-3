import { mount, ReactWrapper } from 'enzyme';
import React from 'react';

import { CorrectionReasonSelect, CorrectionReasonSelectProps } from './CorrectionReasonSelect';
import { CorrectionReasonDTO } from 'src/java/definitions-replenishment';
import { correctionReasons } from 'src/test/data/correctionReasons';
import { Select } from 'src/components';

let wrapper: ReactWrapper<CorrectionReasonSelectProps>;

const items: CorrectionReasonDTO[] = correctionReasons;

describe('<CorrectionReasonSelect />', () => {
  wrapper = mount(<CorrectionReasonSelect items={items} value="1" />);
  wrapper.update();
  it('Should be render correctly', () => {
    const expectedOptions = items.map(({ id, name }) => ({ value: `${id}`, label: name }));
    const expectedValue = expectedOptions.find(item => item.value === '1');

    expect(wrapper.find(CorrectionReasonSelect)).toHaveLength(1);
    expect(wrapper.find(Select).prop('options')).toEqual(expectedOptions);
    expect(wrapper.find(Select).prop('value')).toEqual(expectedValue);
  });
});
