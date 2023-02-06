import { enterFullscreen, exitFullscreen } from 'store/actions/fullscreen';

it('enterFullscreen', () => {
    expect(enterFullscreen()).toMatchSnapshot();
});

it('exitFullscreen', () => {
    expect(exitFullscreen()).toMatchSnapshot();
});
