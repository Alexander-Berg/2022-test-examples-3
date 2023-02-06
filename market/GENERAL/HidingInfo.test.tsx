import * as React from 'react';
import { mount, ReactWrapper } from 'enzyme';

import { Link } from 'src/components';
import { HidingInfo, HidingInfoProps } from '.';

const setupComponent = (comment: string) => {
  const defaultProps: HidingInfoProps = {
    hiding: {
      reasonKey: '',
      shopSku: '',
      supplierId: 0,
      hiddenAt: '',
      shopSkuKey: {
        shopSku: '',
        supplierId: 0,
      },
      comment,
    },
    textPropName: 'shortText',
    showCommonInfo: true,
  };
  const wrapper: ReactWrapper<HidingInfoProps> = mount(<HidingInfo {...defaultProps} />);

  return wrapper;
};

describe('<HidingInfo />', () => {
  it('link in word', () => {
    const wrapper = setupComponent(
      `testcomment1 MARKETASSESSOR-5636 + MARKETSUPPORT-155513
      + MARKETQUALITY-172326 + MARKETANSWERS-204147 -- LEGALMARKET-1957
      MARKETQUALITY-172327 testcomment2  MARKETQUALITY-17aa2326`
    );
    expect(wrapper.find(Link)).toHaveLength(6);
    expect(wrapper.html()).toContain('testcomment1');
    expect(wrapper.html()).toContain('+');
    expect(wrapper.html()).toContain('testcomment2');
  });

  it('link in comment', () => {
    const wrapper = setupComponent('https://st.yandex-team.ru/BERUOPINION-18491');
    expect(wrapper.find(Link)).toHaveLength(1);
  });

  it('MCP link in comment', () => {
    const wrapper = setupComponent('MCP-180682#5e7210e4f6809501ef4d791a');
    expect(wrapper.find(Link)).toHaveLength(1);
  });
});
