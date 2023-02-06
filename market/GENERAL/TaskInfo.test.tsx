import React from 'react';
import { render, screen } from '@testing-library/react';

import { TaskInfo } from './TaskInfo';
import { AuditView } from 'src/pages/TaskAudit/types';
import { columns } from './TaskInfo.constants';

const auditView: AuditView = {
  createdDate: null as any,
  startDate: null as any,
  endDate: null as any,
  inspectorName: null as any,
  inspectorPoolName: null as any,
  operatorName: 'AutoB_1_Шевчук Игорь',
  operatorPoolName: '2019.12.18 MCP-155500 ООО "ТФН" (RS) / Чехлы для мобильных телефонов (91498)',
  columns: null as any,
  root: null as any,
};

describe('TaskInfo::', () => {
  it('should not render the rows with null values', () => {
    render(<TaskInfo {...auditView} />);
    expect(screen.getByText(columns[0].title)).toBeInTheDocument();
    expect(screen.getByText(auditView.operatorPoolName)).toBeInTheDocument();
  });
});
