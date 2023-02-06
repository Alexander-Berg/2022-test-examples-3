import React from 'react';
import { mount } from 'enzyme';

import { column, configureDataTable } from './datatable-config';
import { DataTable } from './DataTable';

const defaultConfig = configureDataTable({
  columns: [column('Column', (item: any) => item.id)],
  keyExtractor: (item: any) => item.id,
});

describe('<DataTable />', () => {
  it('renders without errors', () => {
    mount(<DataTable items={[{ id: 1 }, { id: 2 }]} config={defaultConfig} />);
  });
});
