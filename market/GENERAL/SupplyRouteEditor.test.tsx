import React from 'react';
import { mount, ReactWrapper } from 'enzyme';
import { BrowserRouter } from 'react-router-dom';

import { SupplyRouteEditor } from './SupplyRouteEditor';
import { Select } from 'src/components';
import { SupplyRouteType } from 'src/java/definitions-replenishment';

let wrapper: ReactWrapper | null;
let supplyRoute: SupplyRouteType = SupplyRouteType.DIRECT;

describe('<SupplyRouteEditor/>', () => {
  beforeEach(() => {
    wrapper = mount(
      <BrowserRouter>
        <SupplyRouteEditor
          supplyRoute={supplyRoute}
          supplierId={3}
          onChange={(value: SupplyRouteType) => {
            supplyRoute = value;
          }}
        />
      </BrowserRouter>
    );
  });

  it('should render link', () => {
    expect(wrapper?.find('a')?.getDOMNode().getAttribute('href')).toEqual('/logisticsparams/3/');
  });

  it('should change correctly', () => {
    const button = wrapper?.find('button');
    button?.simulate('click');
    const select = wrapper?.find(Select);
    expect(select?.length).toEqual(1);

    expect(supplyRoute).toEqual(SupplyRouteType.DIRECT);
    select?.props().onChange({ label: '', value: SupplyRouteType.MONO_XDOC });
    expect(supplyRoute).toEqual(SupplyRouteType.MONO_XDOC);
  });
});
