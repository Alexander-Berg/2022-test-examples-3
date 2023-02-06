import React from 'react';
import { render, screen, act } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { useAudioState } from './useAudioState';

class HaveEnoughDataAudioMock extends Audio {
  play = () => {
    this.dispatchEvent(new Event('play'));
    return Promise.resolve();
  };

  pause = () => {
    this.dispatchEvent(new Event('pause'));
  };
}

describe('design/Audio/useAudioState', () => {
  it('passes isLoading', () => {
    const audio = new HaveEnoughDataAudioMock();
    const TestComponent = () => {
      const { isLoading } = useAudioState(audio);

      return <>{isLoading.toString()}</>;
    };

    render(<TestComponent />);

    expect(screen.getByText('true')).toBeInTheDocument();

    act(() => {
      audio.dispatchEvent(new Event('loadeddata'));
    });

    expect(screen.getByText('false')).toBeInTheDocument();
  });

  it('passes isPlaying', () => {
    const audio = new HaveEnoughDataAudioMock();
    const TestComponent = () => {
      const { isPlaying } = useAudioState(audio);

      return <>{isPlaying.toString()}</>;
    };

    act(() => {
      audio.pause();
      audio.dispatchEvent(new Event('pause'));
    });

    render(<TestComponent />);

    expect(screen.getByText('false')).toBeInTheDocument();

    act(() => {
      audio.play();
      audio.dispatchEvent(new Event('play'));
    });

    expect(screen.getByText('true')).toBeInTheDocument();
  });

  it('controls isPlaying with onPlayChange', () => {
    const audio = new HaveEnoughDataAudioMock();
    const TestComponent = () => {
      const { isPlaying, onPlayChange } = useAudioState(audio);

      return <button onClick={() => onPlayChange(true)}>{isPlaying.toString()}</button>;
    };

    act(() => {
      audio.pause();
    });

    render(<TestComponent />);

    expect(screen.getByText('false')).toBeInTheDocument();

    userEvent.click(screen.getByRole('button'));

    expect(screen.getByText('true')).toBeInTheDocument();
  });

  it('passes currentTime', () => {
    const audio = new HaveEnoughDataAudioMock();
    const TestComponent = () => {
      const { currentTime } = useAudioState(audio);

      return <>{currentTime.toString()}</>;
    };

    render(<TestComponent />);

    expect(screen.getByText('0')).toBeInTheDocument();

    act(() => {
      audio.currentTime = 100;
      audio.dispatchEvent(new Event('timeupdate'));
    });

    expect(screen.getByText('100')).toBeInTheDocument();
  });

  it('controls currentTime with onCurrentTimeChange', () => {
    const audio = new HaveEnoughDataAudioMock();
    const TestComponent = () => {
      const { currentTime, onCurrentTimeChange } = useAudioState(audio);

      return <button onClick={() => onCurrentTimeChange(100)}>{currentTime.toString()}</button>;
    };

    render(<TestComponent />);

    expect(screen.getByText('0')).toBeInTheDocument();

    userEvent.click(screen.getByRole('button'));

    expect(screen.getByText('100')).toBeInTheDocument();
  });

  it('passes rate', () => {
    const audio = new HaveEnoughDataAudioMock();
    const TestComponent = () => {
      const { rate } = useAudioState(audio);

      return <>{rate}</>;
    };

    render(<TestComponent />);

    expect(screen.getByText('1')).toBeInTheDocument();

    act(() => {
      audio.playbackRate = 2;
    });

    expect(screen.getByText('2')).toBeInTheDocument();
  });

  it('controls rate with onRateChange', () => {
    const audio = new HaveEnoughDataAudioMock();
    const TestComponent = () => {
      const { rate, onRateChange } = useAudioState(audio);

      return <button onClick={() => onRateChange(3)}>{rate}</button>;
    };

    render(<TestComponent />);

    expect(screen.getByText('1')).toBeInTheDocument();

    userEvent.click(screen.getByRole('button'));

    expect(screen.getByText('3')).toBeInTheDocument();
  });

  it('passes duration', () => {
    const audio = new HaveEnoughDataAudioMock();
    const TestComponent = () => {
      const { duration } = useAudioState(audio);

      return <>{duration}</>;
    };

    render(<TestComponent />);

    expect(screen.getByText('NaN')).toBeInTheDocument();
  });
});
