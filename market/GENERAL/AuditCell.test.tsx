import React from 'react';
import { render, screen } from '@testing-library/react';
import userEvent from '@testing-library/user-event';

import { Align } from 'src/java/definitions';
import { AuditCell } from './AuditCell';
import { ValueType, ComponentType } from 'src/pages/TaskAudit/types';

const items: ComponentType[] = [
  {
    value: '//avatars.mds.yandex.net/get-mpic/1865543/img_id1313893415481635322.jpeg/orig',
    color: 'red',
    valueType: ValueType.IMG,
  },
  {
    value: '//avatars.mds.yandex.net/get-mpic/1567763/img_id6299457942074117117.jpeg/orig',
    color: 'green',
    valueType: ValueType.IMG,
  },
];

const AuditCellInTable = () => (
  <table>
    <tbody>
      <tr>
        <AuditCell items={items} align={Align.LEFT} />
      </tr>
    </tbody>
  </table>
);

describe('AuditCell::', () => {
  it('should render the cell contents', () => {
    render(<AuditCellInTable />);

    expect(screen.getAllByAltText('')).toHaveLength(2);
  });

  it('should render a popup image on click', () => {
    render(<AuditCellInTable />);

    const images = screen.getAllByAltText('');
    userEvent.click(images[0]);
    expect(screen.getAllByAltText('')).toHaveLength(3);
  });
});
