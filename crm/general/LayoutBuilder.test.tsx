import React from 'react';
import { render } from '@testing-library/react';
import { LayoutBuilder } from './LayoutBuilder';
import { LayoutBuilderProps } from './LayoutBuilder.types';

describe('LayoutBuilder', () => {
  it('should render area', () => {
    const data: LayoutBuilderProps = {
      blocks: {
        '1': 'Block',
      },
      layout: {
        schema: {
          rows: [
            {
              areaId: 'Area',
              blockWidthRatio: 1,
            },
          ],
        },
        areaIdToBlocksIds: {
          Area: ['1'],
        },
      },
    };

    const { container } = render(<LayoutBuilder layout={data.layout} blocks={data.blocks} />);

    expect(container).toMatchSnapshot();
  });

  it('should render sub area', () => {
    const data: LayoutBuilderProps = {
      blocks: {
        '1': 'Block2',
      },
      layout: {
        schema: {
          rows: [
            {
              areaId: 'Area1',
              blockWidthRatio: 1,
              columns: [
                {
                  areaId: 'Area2',
                  blockWidthRatio: 1,
                },
              ],
            },
          ],
        },
        areaIdToBlocksIds: {
          Area2: ['1'],
        },
      },
    };

    const { container } = render(<LayoutBuilder layout={data.layout} blocks={data.blocks} />);

    expect(container).toMatchSnapshot();
  });
});
