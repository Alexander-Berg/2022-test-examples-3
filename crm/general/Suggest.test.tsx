import React, { FC, RefObject, ReactElement, useRef, useState } from 'react';
import { render, screen, waitFor } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { RenderProps } from 'components/design/ListItem/Group';
import { Suggest } from './Suggest';
import { Option } from '../Select/Select.types';

const RenderRef: FC<{ children: (ref: RefObject<HTMLDivElement>) => ReactElement }> = ({
  children,
}) => {
  const ref = useRef<HTMLDivElement>(null);

  return <div ref={ref}>{children(ref)}</div>;
};

describe('ReadEdit/Suggest', () => {
  const requiredProps = {
    items: [],
    label: 'label',
    access: 3,
    onEditingStart: jest.fn(),
    onEditingStop: jest.fn(),
  };

  describe('props.label', () => {
    it('renders label', () => {
      render(
        <RenderRef>
          {(ref) => <Suggest {...requiredProps} label="test label" parentRef={ref} />}
        </RenderRef>,
      );

      expect(screen.getByText('test label')).toBeInTheDocument();
    });
  });

  describe('props.value', () => {
    describe('when defined', () => {
      it('renders preview value', () => {
        const items = [
          {
            value: 1,
            name: '1',
          },
        ];
        render(
          <RenderRef>
            {(ref) => (
              <Suggest
                {...requiredProps}
                items={items}
                value={1}
                label="test label"
                parentRef={ref}
              />
            )}
          </RenderRef>,
        );

        expect(screen.getByText('1')).toBeInTheDocument();
      });
    });

    describe('when undefined', () => {
      it('renders dash in preview value', () => {
        const items = [
          {
            value: 1,
            name: '1',
          },
        ];
        render(
          <RenderRef>
            {(ref) => (
              <Suggest {...requiredProps} items={items} label="test label" parentRef={ref} />
            )}
          </RenderRef>,
        );

        expect(screen.getByText('—')).toBeInTheDocument();
      });
    });
  });

  describe('props.placeholder', () => {
    it('renders placeholder in textinput', () => {
      render(
        <RenderRef>
          {(ref) => (
            <Suggest
              {...requiredProps}
              placeholder="Test search"
              isEditing
              label="test label"
              parentRef={ref}
            />
          )}
        </RenderRef>,
      );

      expect(screen.getByPlaceholderText('Test search')).toBeInTheDocument();
    });
  });

  describe('props.text', () => {
    it('renders text in textinput', () => {
      render(
        <RenderRef>
          {(ref) => (
            <Suggest
              {...requiredProps}
              text="Test text"
              isEditing
              label="test label"
              parentRef={ref}
            />
          )}
        </RenderRef>,
      );

      expect(screen.getByDisplayValue('Test text')).toBeInTheDocument();
    });
  });

  describe('props.onTextChange', () => {
    it('calls on textinput change', () => {
      const handleTextChange = jest.fn();
      render(
        <RenderRef>
          {(ref) => (
            <Suggest
              {...requiredProps}
              onTextChange={handleTextChange}
              isEditing
              label="test label"
              parentRef={ref}
            />
          )}
        </RenderRef>,
      );

      const input = screen.getByRole('textbox');
      userEvent.type(input, 'change');

      expect(handleTextChange).toBeCalledTimes(6);
      expect(handleTextChange.mock.calls[handleTextChange.mock.calls.length - 1][0]).toBe('change');
    });
  });

  describe('props.onChange', () => {
    it('calls on change', () => {
      const items = [
        {
          value: 1,
          name: '1',
        },
      ];
      const handleChange = jest.fn();
      render(
        <RenderRef>
          {(ref) => (
            <Suggest
              {...requiredProps}
              isEditing
              onChange={handleChange}
              items={items}
              label="test label"
              parentRef={ref}
            />
          )}
        </RenderRef>,
      );

      userEvent.click(screen.getByText('1'));

      expect(handleChange).toBeCalledWith(1);
    });
  });

  describe('props.isReadLoading', () => {
    describe('when is true', () => {
      it('renders text overlay', () => {
        render(
          <RenderRef>
            {(ref) => (
              <Suggest {...requiredProps} isReadLoading label="test label" parentRef={ref} />
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
              <Suggest
                {...requiredProps}
                isReadLoading={false}
                label="test label"
                parentRef={ref}
              />
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
              <Suggest {...requiredProps} items={items} isEditing isEditLoading parentRef={ref} />
            )}
          </RenderRef>,
        );

        expect(screen.queryByText('1')).not.toBeInTheDocument();
      });

      it('renders spinner', () => {
        render(
          <RenderRef>
            {(ref) => <Suggest {...requiredProps} isEditing isEditLoading parentRef={ref} />}
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
            {(ref) => <Suggest {...requiredProps} isEditing parentRef={ref} />}
          </RenderRef>,
        );

        expect(screen.getByRole('dialog')).toBeInTheDocument();
      });
    });

    describe('when is false', () => {
      it(`doesn't render popup for editing`, () => {
        render(
          <RenderRef>
            {(ref) => <Suggest {...requiredProps} isEditing={false} parentRef={ref} />}
          </RenderRef>,
        );

        expect(screen.queryByRole('dialog')).not.toBeInTheDocument();
      });
    });
  });

  describe('props.onEditingStart', () => {
    it('calls on preview click', () => {
      const handleEditingStart = jest.fn();
      render(
        <RenderRef>
          {(ref) => (
            <Suggest
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
  });

  describe('props.onEditingStop', () => {
    it('calls on outside click', () => {
      const handleEditingStop = jest.fn();
      render(
        <>
          <RenderRef>
            {(ref) => (
              <Suggest
                {...requiredProps}
                onEditingStop={handleEditingStop}
                isEditing
                label="test label"
                parentRef={ref}
              />
            )}
          </RenderRef>
          <div>outside click</div>
        </>,
      );

      userEvent.click(screen.getByText('outside click'));

      expect(handleEditingStop).toBeCalledTimes(1);
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
            <Suggest
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

  describe('polymorphic usage', () => {
    describe('props.items', () => {
      it('uses Option interface by default for items', () => {
        const items: Option[] = [
          {
            value: 1,
            name: 'option',
          },
          {
            value: 2,
            name: 'option',
          },
        ];
        render(
          <RenderRef>
            {(ref) => (
              <Suggest
                {...requiredProps}
                items={items}
                isEditing
                label="test label"
                parentRef={ref}
              />
            )}
          </RenderRef>,
        );

        expect(screen.getAllByText('option')).toHaveLength(2);
      });
    });

    describe('props.renderItem', () => {
      it('renders item using render prop', () => {
        const items = [
          {
            value: 1,
            name: 'option',
          },
          {
            value: 2,
            name: 'option',
          },
        ];
        const renderItem: FC<RenderProps> = (props) => {
          const item = props.item as Option;

          return (
            <div data-testid="render item">
              {item.name} {item.value}
            </div>
          );
        };
        render(
          <RenderRef>
            {(ref) => (
              <Suggest
                {...requiredProps}
                renderItem={renderItem}
                items={items}
                isEditing
                label="test label"
                parentRef={ref}
              />
            )}
          </RenderRef>,
        );

        expect(screen.getAllByTestId('render item')).toHaveLength(2);
      });
    });

    describe('props.getValue', () => {
      it('depends on it to find selected item', () => {
        const items = [
          {
            value: 1,
            name: 'option',
          },
          {
            value: 2,
            name: 'selected option',
          },
        ];
        const getValue = (item: unknown) => (item as Option).value;
        render(
          <RenderRef>
            {(ref) => (
              <Suggest
                {...requiredProps}
                value={2}
                isEditing={false}
                getValue={getValue}
                items={items}
                label="test label"
                parentRef={ref}
              />
            )}
          </RenderRef>,
        );

        expect(screen.getByText('selected option')).toBeInTheDocument();
      });
    });

    describe('props.getName', () => {
      it('depends on it to get preview value', () => {
        const items = [
          {
            value: 1,
            name: 'option',
          },
          {
            value: 2,
            name: 'selected option',
          },
        ];
        const getName = (item: unknown) => (item as Option).name;
        render(
          <RenderRef>
            {(ref) => (
              <Suggest
                {...requiredProps}
                value={2}
                isEditing={false}
                getName={getName}
                items={items}
                label="test label"
                parentRef={ref}
              />
            )}
          </RenderRef>,
        );

        expect(screen.getByText('selected option')).toBeInTheDocument();
      });

      it('allows to set preview value without items preloading', () => {
        const onlyValueLoadedData = {
          value: 2,
          name: 'only value data',
        };
        const items = [];
        const getName = (item: unknown) => {
          if (item && (item as Option).name) {
            return (item as Option).name;
          }

          return onlyValueLoadedData.name;
        };
        render(
          <RenderRef>
            {(ref) => (
              <Suggest
                {...requiredProps}
                value={2}
                isEditing={false}
                getName={getName}
                items={items}
                label="test label"
                parentRef={ref}
              />
            )}
          </RenderRef>,
        );

        expect(screen.getByText('only value data')).toBeInTheDocument();
      });
    });

    it('allows polymorphic usage', async () => {
      const items = [
        {
          id: 1,
          text: 'text 1',
        },
        {
          id: 2,
          text: 'text 2',
        },
      ];
      const handleChange = jest.fn();
      const getName = (item: { id: number; text: string }) => item.text;
      const getValue = (item: { id: number; text: string }) => item.id;
      const renderItem = (props: RenderProps) => {
        const item = props.item as { id: number; text: string };

        return <div onClick={props.onClick}>{item.text}</div>;
      };
      const StatefulSuggest = () => {
        const [isEditing, setEditing] = useState(false);

        const handleEditingStart = () => {
          setEditing(true);
        };

        const handleEditingStop = () => {
          setEditing(false);
        };

        return (
          <RenderRef>
            {(ref) => (
              <Suggest
                {...requiredProps}
                value={2}
                isEditing={isEditing}
                onEditingStart={handleEditingStart}
                onEditingStop={handleEditingStop}
                renderItem={renderItem}
                getName={getName}
                getValue={getValue}
                onChange={handleChange}
                items={items}
                label="test label"
                parentRef={ref}
              />
            )}
          </RenderRef>
        );
      };

      render(<StatefulSuggest />);

      expect(screen.getByText('text 2')).toBeInTheDocument();
      userEvent.click(screen.getByText('text 2'));

      await waitFor(() => {
        screen.getByRole('dialog');
      });

      userEvent.click(screen.getByText('text 1'));

      expect(handleChange).toBeCalledTimes(1);
      expect(handleChange).toBeCalledWith(1);
    });
  });
});
