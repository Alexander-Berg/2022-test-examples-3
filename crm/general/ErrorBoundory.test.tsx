import React from 'react';
import { render, screen } from '@testing-library/react';
import { ErrorBoundary } from './ErrorBoundary';

const customError = new Error('error');

const ComponentWithError = () => {
  throw customError;
};

const ComponentWithoutError = () => <span>ComponentWithoutError</span>;

describe('ErrorBoundary', () => {
  it('catches and renders error', () => {
    render(
      <ErrorBoundary>
        <ComponentWithError />
      </ErrorBoundary>,
    );

    expect(screen.getByText(/Error: error/)).toBeInTheDocument();
  });

  it('renders without error', () => {
    render(
      <ErrorBoundary>
        <ComponentWithoutError />
      </ErrorBoundary>,
    );

    expect(screen.getByText('ComponentWithoutError')).toBeInTheDocument();
  });

  describe('props.renderFallback', () => {
    describe('when defined', () => {
      it('renders fallback', () => {
        render(
          <ErrorBoundary renderFallback={(error) => <div>{error.message}</div>}>
            <ComponentWithError />
          </ErrorBoundary>,
        );

        expect(screen.getByText(customError.message)).toBeInTheDocument();
      });
    });
  });

  describe('props.onError', () => {
    describe('when defined and catched error', () => {
      it('calls with error', () => {
        const onError = jest.fn();
        render(
          <ErrorBoundary onError={onError}>
            <ComponentWithError />
          </ErrorBoundary>,
        );

        expect(onError).toBeCalledWith(customError);
      });
    });
  });
});
