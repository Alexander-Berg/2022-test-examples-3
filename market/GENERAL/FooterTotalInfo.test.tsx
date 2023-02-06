import { mount, ReactWrapper } from 'enzyme';
import React from 'react';

import { createInterWarehouseRecommendationRow } from 'src/test/data/interWarehouseRecommendations';
import { FooterTotalInfo, FooterTotalInfoProps } from './FooterTotalInfo';

let wrapper: ReactWrapper<FooterTotalInfoProps>;

const props: FooterTotalInfoProps = {
  countByTruck: false,
  recommendationsFromTable: [
    createInterWarehouseRecommendationRow({
      msku: 1,
      setQuantity: 5,
      truckQuantity: 1,
      weight: 100,
      length: 10,
      width: 20,
      height: 30,
    }),
    createInterWarehouseRecommendationRow({
      msku: 2,
      setQuantity: 3,
      truckQuantity: 2,
      weight: 200,
      length: 15,
      width: 25,
      height: 35,
    }),
    createInterWarehouseRecommendationRow({
      msku: 3,
      setQuantity: 2,
      truckQuantity: 3,
      weight: 300,
      length: 21,
      width: 22,
      height: 23,
    }),
  ],
};

beforeEach(() => {
  wrapper = mount(<FooterTotalInfo {...props} />);
  wrapper.update();
});

describe('<FooterTotalInfo />', () => {
  it('Should render with setQuantity', () => {
    const totalGridElement = wrapper.find('.totalGrid');
    expect(totalGridElement.childAt(0).text()).toBe('Кол-во: 10 шт.');
    expect(totalGridElement.childAt(1).text()).toBe('MSKU: 3');
    expect(totalGridElement.childAt(2).text()).toBe('Объем: 0.09 м3');
    expect(totalGridElement.childAt(3).text()).toBe('Вес: 1.70 т.');
  });

  it('Should render with truckQuantity', () => {
    wrapper = mount(<FooterTotalInfo {...props} countByTruck />);
    const totalGridElement = wrapper.find('.totalGrid');
    expect(totalGridElement.childAt(0).text()).toBe('Кол-во: 6 шт.');
    expect(totalGridElement.childAt(1).text()).toBe('MSKU: 3');
    expect(totalGridElement.childAt(2).text()).toBe('Объем: 0.06 м3');
    expect(totalGridElement.childAt(3).text()).toBe('Вес: 1.40 т.');
  });
});
