import React, { FC, MutableRefObject } from 'react';
import { render, screen, act } from '@testing-library/react';
import { Form, Field } from 'react-final-form';
import userEvent from '@testing-library/user-event';
import { MergeRef } from 'utils/mergeRef';
import { FormApi } from 'final-form';
import { ForceChangeLocationFlag } from 'services/ForceChangeLocationFlag';
import { createWithLocationChangeAutoSave } from './withLocationChangeAutoSave';
import { WithLocationChangeAutoSaveProps } from './withLocationChangeAutoSave.types';

const WrappedComponent: FC<WithLocationChangeAutoSaveProps> = ({ onSubmit, formApiRef }) => {
  return (
    <Form onSubmit={onSubmit}>
      {({ handleSubmit, form }) => {
        (formApiRef as MutableRefObject<typeof form>).current = form;

        return (
          <form onSubmit={handleSubmit}>
            <Field name="text" initialValue="text" component="input" />
          </form>
        );
      }}
    </Form>
  );
};

const forceChangeLocationFlag = { current: false };

const withLocationChangeAutoSave = createWithLocationChangeAutoSave(
  forceChangeLocationFlag as ForceChangeLocationFlag,
);

const EnhanceWrappedComponent = withLocationChangeAutoSave(WrappedComponent);

const handleSubmit = jest.fn();

describe('withLocationChangeAutoSave', () => {
  beforeEach(() => {
    forceChangeLocationFlag.current = false;
    handleSubmit.mockClear();
  });

  describe('when useLocationChangeAutoSave === false', () => {
    describe('forceChangeLocation.value === false', () => {
      it('does not call submit', () => {
        const { rerender } = render(<EnhanceWrappedComponent onSubmit={handleSubmit} />);
        rerender(<span />);
        expect(handleSubmit).not.toBeCalled();
      });

      it('does not call submit if form change', () => {
        const { rerender } = render(<EnhanceWrappedComponent onSubmit={handleSubmit} />);

        act(() => {
          userEvent.type(screen.getByDisplayValue('text'), 'text1');
        });

        rerender(<span />);
        expect(handleSubmit).not.toBeCalled();
      });
    });

    describe('forceChangeLocation.value === true', () => {
      it('does not call submit', () => {
        const { rerender } = render(<EnhanceWrappedComponent onSubmit={handleSubmit} />);
        rerender(<span />);
        expect(handleSubmit).not.toBeCalled();
      });

      it('does not call submit if form change', () => {
        forceChangeLocationFlag.current = true;
        const mergeRef = new MergeRef<FormApi<object>>();

        const { rerender } = render(
          <EnhanceWrappedComponent onSubmit={handleSubmit} formApiRef={mergeRef} />,
        );

        act(() => {
          userEvent.type(screen.getByDisplayValue('text'), 'text1');
        });

        act(() => {
          rerender(<span />);
        });

        expect(handleSubmit).not.toBeCalledWith({ text: 'text1' }, mergeRef.current);
      });
    });
  });

  describe('when useLocationChangeAutoSave === true', () => {
    describe('forceChangeLocation.value === false', () => {
      it('does not call submit', () => {
        const { rerender } = render(
          <EnhanceWrappedComponent onSubmit={handleSubmit} useLocationChangeAutoSave />,
        );
        rerender(<span />);
        expect(handleSubmit).not.toBeCalled();
      });

      it('does not call submit if form change', () => {
        const { rerender } = render(
          <EnhanceWrappedComponent onSubmit={handleSubmit} useLocationChangeAutoSave />,
        );

        act(() => {
          userEvent.type(screen.getByDisplayValue('text'), 'text1');
        });

        rerender(<span />);
        expect(handleSubmit).not.toBeCalled();
      });
    });

    describe('forceChangeLocation.value === true', () => {
      it('does not call submit', () => {
        const { rerender } = render(
          <EnhanceWrappedComponent onSubmit={handleSubmit} useLocationChangeAutoSave />,
        );
        rerender(<span />);
        expect(handleSubmit).not.toBeCalled();
      });

      it('calls submit if form change', () => {
        forceChangeLocationFlag.current = true;
        const mergeRef = new MergeRef<FormApi<object>>();

        const { rerender } = render(
          <EnhanceWrappedComponent
            onSubmit={handleSubmit}
            formApiRef={mergeRef}
            useLocationChangeAutoSave
          />,
        );

        act(() => {
          userEvent.type(screen.getByDisplayValue('text'), 'text1');
        });

        act(() => {
          rerender(<span />);
        });

        expect(handleSubmit).toBeCalledWith({ text: 'text1' }, mergeRef.current);
      });
    });
  });
});
