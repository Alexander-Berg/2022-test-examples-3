import React from 'react';
import { render, screen, waitFor } from '@testing-library/react';
import { Roller } from './Roller';

HTMLSpanElement.prototype.getBoundingClientRect = jest.fn(() => ({
  height: 100,
  width: 100,
  x: 0,
  y: 0,
  bottom: 0,
  left: 0,
  right: 100,
  top: 0,
  toJSON: () => {},
}));

const ChildComponent = () => <span data-testid={'child'} />;

describe('Roller', () => {
  describe('props.children', () => {
    describe('when defined', () => {
      it('renders children', () => {
        render(
          <Roller>
            <ChildComponent />
            <ChildComponent />
          </Roller>,
        );

        expect(screen.queryAllByTestId('child').length).toBe(2);
      });
    });

    describe('when undefined', () => {
      it("doesn't render children", () => {
        render(<Roller />);

        expect(screen.queryAllByTestId('child').length).toBe(0);
      });
    });
  });

  describe('props.onHiddenComponentsChange', () => {
    describe('when defined', () => {
      it('calls with elements', async () => {
        const onHiddenComponentsChange = jest.fn();

        render(
          <Roller onHiddenComponentsChange={onHiddenComponentsChange}>
            <ChildComponent />
            <ChildComponent />
          </Roller>,
        );

        await waitFor(() => expect(onHiddenComponentsChange.mock.calls[0][0].length).toBe(2));
      });

      it('calls with elements inside fragment', async () => {
        const onHiddenComponentsChange = jest.fn();

        render(
          <Roller onHiddenComponentsChange={onHiddenComponentsChange}>
            <>
              <ChildComponent />
              <ChildComponent />
            </>
          </Roller>,
        );

        await waitFor(() => expect(onHiddenComponentsChange.mock.calls[0][0].length).toBe(2));
      });

      it('calls with elements inside context', async () => {
        const onHiddenComponentsChange = jest.fn();
        const SomeContext = React.createContext('');

        render(
          <Roller onHiddenComponentsChange={onHiddenComponentsChange}>
            <SomeContext.Provider value="">
              <ChildComponent />
              <SomeContext.Consumer>
                {() =>
                  [ChildComponent, ChildComponent].map((Child, index) => <Child key={index} />)
                }
              </SomeContext.Consumer>
            </SomeContext.Provider>
          </Roller>,
        );

        await waitFor(() => expect(onHiddenComponentsChange.mock.calls[0][0].length).toBe(3));
      });
    });
  });

  describe('props.updateKey', () => {
    describe('when defined', () => {
      describe('when changed', () => {
        it('splits children', async () => {
          const onHiddenComponentsChange = jest.fn();
          const { rerender } = render(
            <Roller updateKey="1" onHiddenComponentsChange={onHiddenComponentsChange} />,
          );

          await waitFor(() => expect(onHiddenComponentsChange).toBeCalledTimes(1));

          rerender(<Roller updateKey="2" onHiddenComponentsChange={onHiddenComponentsChange} />);

          await waitFor(() => expect(onHiddenComponentsChange).toBeCalledTimes(2));
        });
      });

      describe('when not changed', () => {
        it("doesn't split children", async () => {
          const onHiddenComponentsChange = jest.fn();
          const { rerender } = render(
            <Roller updateKey="1" onHiddenComponentsChange={onHiddenComponentsChange} />,
          );

          await waitFor(() => expect(onHiddenComponentsChange).toBeCalledTimes(1));

          rerender(<Roller updateKey="1" onHiddenComponentsChange={onHiddenComponentsChange} />);

          await waitFor(() => expect(onHiddenComponentsChange).toBeCalledTimes(1));
        });
      });
    });

    describe('when undefined', () => {
      describe('when children changed', () => {
        it('splits children', async () => {
          const onHiddenComponentsChange = jest.fn();
          let children = <ChildComponent />;

          const { rerender } = render(
            <Roller onHiddenComponentsChange={onHiddenComponentsChange}>{children}</Roller>,
          );

          await waitFor(() => expect(onHiddenComponentsChange).toBeCalledTimes(1));

          children = <ChildComponent />;

          rerender(<Roller onHiddenComponentsChange={onHiddenComponentsChange}>{children}</Roller>);

          await waitFor(() => expect(onHiddenComponentsChange).toBeCalledTimes(2));
        });
      });

      describe('when children not changed', () => {
        it("doesn't split children", async () => {
          const onHiddenComponentsChange = jest.fn();
          let children = <ChildComponent />;

          const { rerender } = render(
            <Roller onHiddenComponentsChange={onHiddenComponentsChange}>{children}</Roller>,
          );

          await waitFor(() => expect(onHiddenComponentsChange).toBeCalledTimes(1));

          rerender(<Roller onHiddenComponentsChange={onHiddenComponentsChange}>{children}</Roller>);

          await waitFor(() => expect(onHiddenComponentsChange).toBeCalledTimes(1));
        });
      });
    });
  });
});
