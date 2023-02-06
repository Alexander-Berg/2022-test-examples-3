import React, { useEffect, MutableRefObject } from 'react';
import { render, act, RenderResult } from '@testing-library/react';
import { createForm, FormApi } from 'final-form';
import { withAutoSave, WithAutoSaveWrappedProps } from './withAutoSave';
import { CustomFormApi } from './withCustomSubmit';
import { FormValues } from '../NewMailForm.types';

const mockCustomFormApiSubmit = jest.fn();
const mockCreateForm = jest.fn(() => {
  const form = createForm({
    initialValues: { text: 'text' } as FormValues,
    onSubmit: () => {},
  });

  form.registerField('text', () => {}, { value: true });

  return form;
});

const WithAutoSaveWrappedComponent = jest.fn((props: WithAutoSaveWrappedProps) => {
  useEffect(() => {
    (props.formApiRef as MutableRefObject<FormApi<FormValues>>)!.current = mockCreateForm();

    (props.customFormApiRef as MutableRefObject<CustomFormApi>)!.current = {
      submit: mockCustomFormApiSubmit,
    };

    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, []);

  return null;
});

jest.useFakeTimers('modern');

describe('NewMailForm', () => {
  describe('withAutoSave', () => {
    beforeEach(() => {
      mockCustomFormApiSubmit.mockClear();
      WithAutoSaveWrappedComponent.mockClear();
      mockCreateForm.mockClear();
    });

    it('should not autosave if pristine in one minute', () => {
      const WithAutoSave = withAutoSave(WithAutoSaveWrappedComponent);
      render(<WithAutoSave hasAutoSave />);

      jest.advanceTimersByTime(1000 * 60);

      expect(mockCustomFormApiSubmit).not.toBeCalled();
    });

    it('should autosave if values have been changed in one minute', () => {
      const WithAutoSave = withAutoSave(WithAutoSaveWrappedComponent);
      act(() => {
        render(<WithAutoSave hasAutoSave />);
      });

      const formApi = mockCreateForm.mock.results[0].value as FormApi<FormValues>;

      act(() => {
        formApi.change('text', 'text2');

        jest.advanceTimersByTime(1000 * 60);
      });

      expect(mockCustomFormApiSubmit).toBeCalled();
      expect(mockCustomFormApiSubmit.mock.calls[0][0].action).toBe('autoSave');

      act(() => {
        mockCustomFormApiSubmit.mock.calls[0][0].onSubmitSuccess();
      });

      const autosaveDate = new Date();
      jest.setSystemTime(autosaveDate);

      expect(
        WithAutoSaveWrappedComponent.mock.calls[
          WithAutoSaveWrappedComponent.mock.calls.length - 1
        ][0].autoSaveDate!.toString(),
      ).toBe(autosaveDate.toString());
    });

    it('should not autosave if not provider hasAutoSave prop', () => {
      const WithAutoSave = withAutoSave(WithAutoSaveWrappedComponent);
      act(() => {
        render(<WithAutoSave />);
      });

      const formApi = mockCreateForm.mock.results[0].value as FormApi<FormValues>;

      act(() => {
        formApi.change('text', 'text2');

        jest.advanceTimersByTime(1000 * 60);
      });

      expect(mockCustomFormApiSubmit).not.toBeCalled();
    });

    it('should not autosave on unmount without autoSaveOnUnmount prop', () => {
      const WithAutoSave = withAutoSave(WithAutoSaveWrappedComponent);

      act(() => {
        render(<WithAutoSave />);
      });

      act(() => {
        render(<span>empty</span>);
      });

      expect(mockCustomFormApiSubmit).not.toBeCalled();
    });

    it('should not autosave on unmount if pristine', () => {
      const WithAutoSave = withAutoSave(WithAutoSaveWrappedComponent);

      act(() => {
        render(<WithAutoSave autoSaveOnUnmount />);
      });

      act(() => {
        render(<span>empty</span>);
      });

      expect(mockCustomFormApiSubmit).not.toBeCalled();
    });

    it('should autosave on unmount if values have been changed', () => {
      const WithAutoSave = withAutoSave(WithAutoSaveWrappedComponent);

      let root: RenderResult;
      act(() => {
        root = render(<WithAutoSave autoSaveOnUnmount />);
      });

      const formApi = mockCreateForm.mock.results[0].value as FormApi<FormValues>;

      act(() => {
        formApi.change('text', 'text2');
      });

      act(() => {
        root.rerender(<span>empty</span>);
      });

      expect(mockCustomFormApiSubmit).toBeCalled();
    });
  });
});
