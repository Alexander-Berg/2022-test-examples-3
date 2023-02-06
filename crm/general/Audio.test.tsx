import React from 'react';
import userEvent from '@testing-library/user-event';
import { render, waitFor, screen } from '@testing-library/react';
import { TestBed } from 'components/TestBed';
import { Audio } from './Audio';

beforeAll(() => {
  require('./setupJest');
});

describe('Audio', () => {
  describe('when is loading', () => {
    it('renders loading overlay', async () => {
      render(
        <TestBed>
          <Audio src="" />
        </TestBed>,
      );

      expect(screen.getByTestId('loading')).toBeInTheDocument();
    });
  });

  describe('when is loaded', () => {
    it('renders audio player', async () => {
      render(
        <TestBed>
          <Audio src="" />
        </TestBed>,
      );
      const audio = screen.getByTestId('audio');
      audio.dispatchEvent(new Event('loadeddata'));

      expect(screen.queryByTestId('loading')).not.toBeInTheDocument();
      expect(screen.getByTestId('btn-play')).toBeInTheDocument();
      expect(screen.getByTestId('btn-rate')).toBeInTheDocument();
    });
  });

  describe('when click play', () => {
    it('plays audio', async () => {
      render(
        <TestBed>
          <Audio src="" />
        </TestBed>,
      );
      const audio = screen.getByTestId('audio') as HTMLAudioElement;
      audio.dispatchEvent(new Event('loadeddata'));

      const buttonPlay = screen.getByTestId('btn-play');

      await waitFor(() => expect(audio.paused).toBe(true));

      userEvent.click(buttonPlay);
      await waitFor(() => expect(audio.paused).toBe(false));

      userEvent.click(buttonPlay);
      await waitFor(() => expect(audio.paused).toBe(true));
    });
  });

  describe('when click change rate', () => {
    it('plays audio faster', () => {
      render(
        <TestBed>
          <Audio src="" />
        </TestBed>,
      );
      const audio = screen.getByTestId('audio') as HTMLAudioElement;

      audio.dispatchEvent(new Event('loadeddata'));

      const buttonRate = screen.getByTestId('btn-rate');
      expect(buttonRate.textContent).toBe('1x');

      userEvent.click(buttonRate);
      expect(buttonRate.textContent).toBe('1.5x');

      userEvent.click(buttonRate);
      expect(buttonRate.textContent).toBe('2x');

      userEvent.click(buttonRate);
      expect(buttonRate.textContent).toBe('1x');
    });
  });

  describe('props.error', () => {
    it('renders error.message', () => {
      render(
        <TestBed>
          <Audio src="" error={{ message: 'test message' }} />
        </TestBed>,
      );

      expect(screen.getByText('test message')).toBeInTheDocument();
    });

    it('renders retry button when error.canRetry is true', () => {
      render(
        <TestBed>
          <Audio src="" error={{ canRetry: true }} />
        </TestBed>,
      );

      expect(screen.getByText('Попробовать еще раз')).toBeInTheDocument();
    });

    it(`doesn't render retry button when error.canRetry is false`, () => {
      render(
        <TestBed>
          <Audio src="" error={{ canRetry: false }} />
        </TestBed>,
      );

      expect(screen.queryByText('Попробовать еще раз')).not.toBeInTheDocument();
    });
  });

  describe('props.onRetry', () => {
    describe('when prevents reloading', () => {
      it(`doesn't reload`, () => {
        const handleRetry = () => false;
        render(
          <TestBed>
            <Audio src="" error={{ message: 'test' }} onRetry={handleRetry} />
          </TestBed>,
        );

        const audio = screen.getByTestId('audio') as HTMLAudioElement;
        userEvent.click(screen.getByText('Попробовать еще раз'));

        expect(audio.load).toBeCalledTimes(1);
      });
    });

    describe('when allows reloading', () => {
      it('reloads', () => {
        const handleRetry = () => true;
        render(
          <TestBed>
            <Audio src="" error={{ message: 'test' }} onRetry={handleRetry} />
          </TestBed>,
        );

        const audio = screen.getByTestId('audio') as HTMLAudioElement;
        userEvent.click(screen.getByText('Попробовать еще раз'));

        expect(audio.load).toBeCalledTimes(2);
      });
    });
  });
});
