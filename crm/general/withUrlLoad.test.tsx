import React, { FC, ReactElement, RefObject, useRef } from 'react';
import { render, waitFor, screen } from '@testing-library/react';
import { rest } from 'msw';
import { setupServer } from 'msw/node';
import { withUrlLoad } from './withUrlLoad';
import { Suggest as PureSuggest } from '../../Suggest';

const server = setupServer(
  rest.get('/options', (req, res, ctx) =>
    res(
      ctx.json([
        {
          value: 1,
          name: 'option',
        },
        {
          value: 2,
          name: 'option',
        },
      ]),
    ),
  ),
  rest.get('/items', (req, res, ctx) =>
    res(
      ctx.json({
        items: [
          {
            id: 1,
            text: 'item',
          },
          {
            id: 2,
            text: 'item',
          },
        ],
      }),
    ),
  ),
);

const RenderRef: FC<{ children: (ref: RefObject<HTMLDivElement>) => ReactElement }> = ({
  children,
}) => {
  const ref = useRef<HTMLDivElement>(null);

  return <div ref={ref}>{children(ref)}</div>;
};

describe('hoc/withUrlLoad', () => {
  beforeAll(() => {
    server.listen();
  });

  afterAll(() => {
    server.close();
  });

  const requiredProps = {
    access: 3,
    label: 'label',
    onEditingStart: jest.fn(),
    onEditingStop: jest.fn(),
  };

  const Suggest = withUrlLoad(PureSuggest);

  describe('props.url', () => {
    it('loads items from url', async () => {
      render(
        <RenderRef>
          {(ref) => <Suggest {...requiredProps} isEditing url="/options" parentRef={ref} />}
        </RenderRef>,
      );

      await waitFor(() => {
        expect(screen.getAllByText('option')).toHaveLength(2);
      });
    });
  });

  describe('props.mapper', () => {
    it('allows to map response data to items', async () => {
      const mapper = (data) =>
        (data as { items: { id: number; text: string }[] }).items.map((item) => ({
          value: item.id,
          name: item.text,
        }));
      render(
        <RenderRef>
          {(ref) => (
            <Suggest {...requiredProps} mapper={mapper} isEditing url="/items" parentRef={ref} />
          )}
        </RenderRef>,
      );

      await waitFor(() => {
        expect(screen.getAllByText('item')).toHaveLength(2);
      });
    });
  });
});
