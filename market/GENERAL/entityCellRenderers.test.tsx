import React from 'react';
import { render, screen } from '@testing-library/react';

import { AttrExternalReferenceIdRenderer } from './AttrExternalReferenceIdRenderer';
import { IGridDataCellParams, IGridDataRow } from '../types';
import { CommonEntity, MdmProtocol } from '../../../java/definitions';
import { BooleanExternalReferenceIdRenderer } from './BooleanExternalReferenceIdRenderer';
import { EntityTypeNameRenderer } from './EntityTypeNameRenderer';
import { EnumExternalReferenceIdRenderer } from './EnumExternalReferenceIdRenderer';
import { EnumOptionReferenceRenderer } from './EnumOptionReferenceRenderer';
import { SskuNameRenderer } from './SskuNameRenderer';
import { ViewSettingsIdRenderer } from './ViewSettingsIdRenderer';
import { DefaultCellRenderer } from './DefaultCellRenderer';
import { StructCellRenderer } from './StructCellRenderer';
import { ReferenceCellRenderer } from './ReferenceCellRenderer';
import { MdmEntityIdRenderer } from './MdmEntityIdRenderer';
import { COMMON_PARAM_NAMES_MAP } from 'src/models/api.model';

describe('entityCellRenderers', () => {
  const param = {
    formattedValue: 'Значение',
    value: 'val1',
    row: { id: 123, commonEntity: { entityId: 234 } as unknown as CommonEntity } as IGridDataRow,
  } as IGridDataCellParams;

  it('<AttrExternalReferenceIdRenderer />', () => {
    render(<AttrExternalReferenceIdRenderer {...param} />);
    expect(screen.getByText('Значение')).toHaveAttribute('href', '/bmdm/w/view/MDM_ATTR_EXTERNAL_REFERENCE/234');
  });

  it('<BooleanExternalReferenceIdRenderer />', () => {
    render(<BooleanExternalReferenceIdRenderer {...param} />);
    expect(screen.getByText('Значение')).toHaveAttribute('href', '/bmdm/w/view/MDM_BOOLEAN_EXTERNAL_REFERENCE/234');
  });

  it('<EntityTypeNameRenderer />', () => {
    render(<EntityTypeNameRenderer {...param} />);
    expect(screen.getByText('Значение')).toHaveAttribute('href', '/constructor/mdm-entity-type/234');
  });

  it('<EnumExternalReferenceIdRenderer />', () => {
    render(<EnumExternalReferenceIdRenderer {...param} />);
    expect(screen.getByText('Значение')).toHaveAttribute('href', '/bmdm/w/view/MDM_ENUM_OPTION_EXTERNAL_REFERENCE/234');
  });

  it('<EnumOptionReferenceRenderer />', () => {
    render(<EnumOptionReferenceRenderer {...param} />);
    expect(screen.getByText('Значение')).toHaveAttribute('href', '/bmdm/w/view/MDM_ENUM_OPTION/234');
  });

  it('<SskuNameRenderer />', () => {
    render(<SskuNameRenderer {...param} />);
    expect(screen.getByText('Значение')).toHaveAttribute('href', '/bmdm/w/view/SSKU/234');
  });

  it('<ViewSettingsIdRenderer />', () => {
    render(<ViewSettingsIdRenderer {...param} />);
    expect(screen.getByText('val1')).toHaveAttribute('href', '/bmdm/w/view/COMMON_PARAM_VIEW_SETTING/234');
  });

  it('<DefaultCellRenderer />', () => {
    render(<DefaultCellRenderer {...param} />);
    expect(screen.getByText('Значение')).toBeInTheDocument();
  });

  it('<ReferenceCellRenderer />', () => {
    render(<ReferenceCellRenderer {...param} value={{ mdmId: 123, mdmEntityTypeId: 456 }} />);
    expect(screen.getByText('Значение')).toHaveAttribute('href', '/bmdm/w/view/MDM_ENTITY/123?entityTypeId=456');
  });

  it('<StructCellRenderer />', () => {
    const app = render(<StructCellRenderer {...param} />);
    expect(app.getByText('Значение')).toHaveAttribute('href', '/constructor/mdm-entity-type/val1');

    app.rerender(<StructCellRenderer {...param} value={undefined as any} />);
    expect(app.getByText('Значение')).not.toHaveAttribute('href');
  });

  it('<MdmEntityIdRenderer />', () => {
    const commonEntity = {
      entityId: 123,
      commonParamValues: [
        {
          commonParamName: MdmProtocol.REQUEST_ENTITY_ID,
          numerics: [1],
        },
        {
          commonParamName: COMMON_PARAM_NAMES_MAP.entity_type_id,
          numerics: [22],
        },
      ],
    } as CommonEntity;
    const app = render(<MdmEntityIdRenderer {...param} row={{ id: 123, metaCommonParams: [], commonEntity }} />);
    expect(app.getByText('val1')).toHaveAttribute('href', '/bmdm/w/view/MDM_ENTITY/1?entityTypeId=22');
  });
});
