import React from 'react';
import { shallow } from 'enzyme';

import { Spin } from 'src/components';
import { LoaderBox } from './LoaderBox';

describe('<LoaderBox />', () => {
  it('render', () => {
    expect(shallow(<LoaderBox>test</LoaderBox>).html()).toBe(
      '<div class="wrapper">test<div class="backGround"></div></div>'
    );

    expect(shallow(<LoaderBox isLoading>test</LoaderBox>).find('.active')).toHaveLength(1);

    expect(
      shallow(
        <LoaderBox isLoading fixed dark>
          test
        </LoaderBox>
      ).find('.active.fixed.dark')
    ).toHaveLength(1);
  });

  it('render only spin', () => {
    const wrap = shallow(<LoaderBox size="s">test</LoaderBox>);
    expect(wrap.find(Spin)).toHaveLength(1);
    expect(wrap.find('.backGround').children()).toHaveLength(1);
  });

  it('render only text', () => {
    const wrap = shallow(<LoaderBox loader="loading...">test</LoaderBox>);
    expect(wrap.find(Spin)).toHaveLength(0);
    expect(wrap.find('.backGround').children()).toHaveLength(1);
    expect(wrap.find('.backGround').text()).toEqual('loading...');
  });

  it('render spin and text', () => {
    const wrap = shallow(
      <LoaderBox size="s" loader="loading...">
        test
      </LoaderBox>
    );
    expect(wrap.find(Spin)).toHaveLength(1);
    expect(wrap.find('.backGround').children()).toHaveLength(2);
    expect(wrap.find('.backGround .label').text()).toEqual('loading...');
  });

  it('render spin and ReactElement', () => {
    const re = <strong>loading...</strong>;
    const wrap = shallow(
      <LoaderBox size="s" loader={re}>
        test
      </LoaderBox>
    );
    expect(wrap.find(Spin)).toHaveLength(1);
    expect(wrap.find('.backGround').children()).toHaveLength(2);
    expect(wrap.find('.backGround .label').childAt(0).getElement()).toEqual(re);
  });
});
