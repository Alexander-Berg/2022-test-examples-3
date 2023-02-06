import React from 'react';
import { render, screen, fireEvent, cleanup } from '@testing-library/react';
import '@testing-library/jest-dom/extend-expect';
import UXExperiment from 'services/Rum/UXExperiment';
import { mocked } from 'ts-jest/utils';
import UxExperimentSplitter from './UxExperimentSplitter';
jest.mock('services/Rum/UXExperiment');
jest.mock('../AttributeWithPopup', () => (props) => (
  <div data-testid="wrappedComponent">
    <div onClick={props.onLabelClick}>Label</div>
    <div onClick={props.onChange}>Change</div>
    <div onClick={props.onCancel}>Cancel</div>
  </div>
));
const MockUXExperiment = mocked(UXExperiment, true);
const mockProps = {
  issueId: '34576',
  name: 'queue',
  label: 'Label',
  redux: jest.fn(),
  currentIssueInfo: {},
  access: 3,
  component: 'Suggest',
  onLabelClick: jest.fn(),
  onChange: jest.fn(),
  onCancel: jest.fn(),
};

let mockExperiment;
describe('AttributeWithPopupAndUxExperiment', () => {
  afterEach(() => {
    MockUXExperiment.mockClear();
    mockProps.onLabelClick.mockClear();
    mockProps.onChange.mockClear();
    mockProps.onCancel.mockClear();
    cleanup();
  });
  describe('renders component', () => {
    it('adds to document', async () => {
      render(<UxExperimentSplitter {...mockProps} />);
      mockExperiment = MockUXExperiment.mock.instances[0];
      expect(screen.getByTestId('wrappedComponent')).toBeInTheDocument();
    });
    describe('when component in experiment', () => {
      beforeEach(() => {
        render(<UxExperimentSplitter {...mockProps} />);
      });

      describe('clicks on label', () => {
        beforeEach(() => {
          fireEvent.click(screen.getByText('Label'));
        });
        it('calls "onLabelClick" props', async () => {
          expect(mockProps.onLabelClick).toBeCalled();
        });
      });
    });

    describe('when component not in experiment', () => {
      beforeEach(() => {
        const changedMockProps = { ...mockProps, name: 'notQueue' };
        render(<UxExperimentSplitter {...changedMockProps} />);
        mockExperiment = MockUXExperiment.mock.instances[0];
      });
      it(`don't init experiment`, async () => {
        expect(mockExperiment).toEqual(undefined);
      });
      describe('clicks on label', () => {
        it('calls "onLabelClick" props', async () => {
          fireEvent.click(screen.getByText('Label'));
          expect(mockProps.onLabelClick).toBeCalled();
        });
      });
      describe('clicks on some value', () => {
        it('calls "onChange" props', async () => {
          fireEvent.click(screen.getByText('Change'));
          expect(mockProps.onChange).toBeCalled();
        });
      });
      describe('clicks on Cancel', () => {
        it('calls "onCancel" props', async () => {
          fireEvent.click(screen.getByText('Cancel'));
          expect(mockProps.onCancel).toBeCalled();
        });
      });
    });
  });
});
