import React, { RefObject, ReactElement, useRef } from 'react';
import { render, screen } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { Select } from './Select';

const RenderRef = ({
  children,
}: {
  children: (ref: RefObject<HTMLDivElement>) => ReactElement;
}) => {
  const ref = useRef<HTMLDivElement>(null);

  return <div ref={ref}>{children(ref)}</div>;
};

describe('ReadEdit/Select', () => {
  const requiredProps = {
    label: 'label',
    items: [],
    access: 3,
    onEditingStart: jest.fn(),
    onEditingStop: jest.fn(),
  };

  describe('props.label', () => {
    it('renders label', () => {
      render(
        <RenderRef>
          {(ref) => <Select {...requiredProps} label="test label" parentRef={ref} />}
        </RenderRef>,
      );

      expect(screen.getByText('test label')).toBeInTheDocument();
    });
  });

  describe('props.items', () => {
    it('renders items', () => {
      const items = [
        {
          value: 1,
          name: 'test option',
        },
        {
          value: 2,
          name: 'test option',
        },
      ];
      render(
        <RenderRef>
          {(ref) => (
            <Select {...requiredProps} items={items} isEditing label="test label" parentRef={ref} />
          )}
        </RenderRef>,
      );

      expect(screen.getAllByText('test option')).toHaveLength(2);
    });
  });

  describe('props.value', () => {
    describe('when undefined', () => {
      it('renders dash in preview value', () => {
        const items = [
          {
            value: 1,
            name: '1',
          },
          {
            value: 2,
            name: '2',
          },
        ];
        render(
          <RenderRef>
            {(ref) => (
              <Select
                {...requiredProps}
                items={items}
                value={undefined}
                label="test label"
                parentRef={ref}
              />
            )}
          </RenderRef>,
        );

        expect(screen.getByText('—')).toBeInTheDocument();
      });
    });

    describe('when defined', () => {
      it('renders preview value', () => {
        const items = [
          {
            value: 1,
            name: '1',
          },
          {
            value: 2,
            name: '2',
          },
        ];
        render(
          <RenderRef>
            {(ref) => (
              <Select
                {...requiredProps}
                items={items}
                value={2}
                label="test label"
                parentRef={ref}
              />
            )}
          </RenderRef>,
        );

        expect(screen.getByText('2')).toBeInTheDocument();
      });
    });
  });

  describe('props.onChange', () => {
    it('calls on change', () => {
      const items = [
        {
          value: 1,
          name: '1',
        },
        {
          value: 2,
          name: '2',
        },
      ];
      const handleChange = jest.fn();
      render(
        <RenderRef>
          {(ref) => (
            <Select
              {...requiredProps}
              items={items}
              isEditing
              label="test label"
              onChange={handleChange}
              parentRef={ref}
            />
          )}
        </RenderRef>,
      );

      userEvent.click(screen.getByText('2'));

      expect(handleChange).toBeCalledWith(2);
    });
  });

  describe('props.isReadLoading', () => {
    describe('when is true', () => {
      it('renders text overlay', () => {
        render(
          <RenderRef>
            {(ref) => (
              <Select {...requiredProps} isReadLoading label="test label" parentRef={ref} />
            )}
          </RenderRef>,
        );

        expect(screen.getByText('Сохранение изменений...')).toBeInTheDocument();
      });
    });

    describe('when is false', () => {
      it(`doesn't render text overlay`, () => {
        render(
          <RenderRef>
            {(ref) => (
              <Select {...requiredProps} isReadLoading={false} label="test label" parentRef={ref} />
            )}
          </RenderRef>,
        );

        expect(screen.queryByText('Сохранение изменений...')).not.toBeInTheDocument();
      });
    });
  });

  describe('props.isEditLoading', () => {
    describe('when is true', () => {
      it(`doesn't render items`, () => {
        const items = [
          {
            value: 1,
            name: '1',
          },
        ];
        render(
          <RenderRef>
            {(ref) => (
              <Select
                {...requiredProps}
                items={items}
                isEditing
                isEditLoading
                label="test label"
                parentRef={ref}
              />
            )}
          </RenderRef>,
        );

        expect(screen.queryByText('1')).not.toBeInTheDocument();
      });

      it('renders spinner', () => {
        render(
          <RenderRef>
            {(ref) => (
              <Select
                {...requiredProps}
                isEditing
                isEditLoading
                label="test label"
                parentRef={ref}
              />
            )}
          </RenderRef>,
        );

        expect(screen.getByRole('alert')).toBeInTheDocument();
      });
    });
  });

  describe('props.isEditing', () => {
    describe('when is true', () => {
      it('renders popup for editing', () => {
        render(
          <RenderRef>
            {(ref) => <Select {...requiredProps} isEditing label="test label" parentRef={ref} />}
          </RenderRef>,
        );

        expect(screen.getByRole('dialog')).toBeInTheDocument();
      });
    });

    describe('when is false', () => {
      it(`doesn't render popup for editing`, () => {
        render(
          <RenderRef>
            {(ref) => (
              <Select {...requiredProps} isEditing={false} label="test label" parentRef={ref} />
            )}
          </RenderRef>,
        );

        expect(screen.queryByRole('dialog')).not.toBeInTheDocument();
      });
    });
  });

  describe('props.onEditingStart', () => {
    it('calls if isEditing = false', () => {
      const handleEditingStart = jest.fn();
      render(
        <RenderRef>
          {(ref) => (
            <Select
              {...requiredProps}
              onEditingStart={handleEditingStart}
              isEditing={false}
              label="test label"
              parentRef={ref}
            />
          )}
        </RenderRef>,
      );

      userEvent.click(screen.getByText('test label'));

      expect(handleEditingStart).toBeCalledTimes(1);
    });

    it(`doesn't call if isEditing = true`, () => {
      const handleEditingStart = jest.fn();
      render(
        <RenderRef>
          {(ref) => (
            <Select
              {...requiredProps}
              onEditingStart={handleEditingStart}
              isEditing
              label="test label"
              parentRef={ref}
            />
          )}
        </RenderRef>,
      );

      userEvent.click(screen.getByText('test label'));

      expect(handleEditingStart).not.toBeCalledTimes(1);
    });
  });

  describe('props.onEditingStop', () => {
    it('calls if isEditing = true', () => {
      const handleEditingStop = jest.fn();
      render(
        <RenderRef>
          {(ref) => (
            <Select
              {...requiredProps}
              onEditingStop={handleEditingStop}
              isEditing
              label="test label"
              parentRef={ref}
            />
          )}
        </RenderRef>,
      );

      userEvent.click(screen.getByText('test label'));

      expect(handleEditingStop).toBeCalledTimes(1);
    });

    it(`doesn't call if isEditing = false`, () => {
      const handleEditingStop = jest.fn();
      render(
        <RenderRef>
          {(ref) => (
            <Select
              {...requiredProps}
              onEditingStop={handleEditingStop}
              isEditing={false}
              label="test label"
              parentRef={ref}
            />
          )}
        </RenderRef>,
      );

      userEvent.click(screen.getByText('test label'));

      expect(handleEditingStop).not.toBeCalledTimes(1);
    });

    it('calls after change', () => {
      const items = [
        {
          value: 1,
          name: '1',
        },
      ];
      const handleChange = jest.fn();
      const handleEditingStop = jest.fn();
      render(
        <RenderRef>
          {(ref) => (
            <Select
              {...requiredProps}
              items={items}
              onChange={handleChange}
              onEditingStop={handleEditingStop}
              isEditing
              label="test label"
              parentRef={ref}
            />
          )}
        </RenderRef>,
      );

      userEvent.click(screen.getByText('1'));

      expect(handleEditingStop).toBeCalledTimes(1);
    });
  });
});
