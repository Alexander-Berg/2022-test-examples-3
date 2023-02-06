import React from 'react';
import { mount } from 'enzyme';

import { PartnerRelation, PartnerRelationType, Warehouse, WarehouseType } from 'src/java/definitions';
import { PartnerRelationInfo } from '.';

const partnerRelation: PartnerRelation = {
  fromWarehouseIds: [123],
  id: 1234,
  modifiedTs: '123',
  relationType: PartnerRelationType.CROSSDOCK,
  supplierId: 12345,
  toWarehouseId: 321,
};

const fromWarehouse: Warehouse = {
  id: partnerRelation.fromWarehouseIds[0],
  modifiedAt: '123',
  name: 'first warehouse',
  type: WarehouseType.DROPSHIP,
};

describe('<PartnerRelationInfo> unit test', () => {
  it('should be render without errors', () => {
    expect(() => {
      mount(<PartnerRelationInfo partnerRelation={partnerRelation} />);
    }).not.toThrow();
  });

  it('main content flow', () => {
    const component = mount(<PartnerRelationInfo partnerRelation={partnerRelation} fromWarehouse={fromWarehouse} />);
    const expected = `#${partnerRelation.fromWarehouseIds}(${fromWarehouse.name})→#${partnerRelation.toWarehouseId}`;
    expect(component.text()).toEqual(expected);
  });
});
