import React from 'react';
import { render, screen } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { PlayButton } from './PlayButton';
import { playIconTestId, pauseIconTestId } from './PlayButton.constants';

describe('design/Audio/PlayButton', () => {
  it('shows play icon when isPlaying=false', () => {
    render(<PlayButton isPlaying={false} />);

    expect(screen.getByTestId(playIconTestId)).toBeInTheDocument();
  });

  it('shows pause icon when isPlaying=true', () => {
    render(<PlayButton isPlaying />);

    expect(screen.getByTestId(pauseIconTestId)).toBeInTheDocument();
  });

  it('disables button when isDisabled=true', () => {
    render(<PlayButton isPlaying isDisabled />);

    expect(screen.getByRole('button')).toBeDisabled();
  });

  it('calls onPlayChange with toggled value', () => {
    const handlePlayChange = jest.fn(() => {});

    const { rerender } = render(<PlayButton isPlaying onPlayChange={handlePlayChange} />);

    userEvent.click(screen.getByRole('button'));

    expect(handlePlayChange).toBeCalledWith(false);

    rerender(<PlayButton isPlaying={false} onPlayChange={handlePlayChange} />);

    userEvent.click(screen.getByRole('button'));

    expect(handlePlayChange).toBeCalledWith(true);
  });
});
