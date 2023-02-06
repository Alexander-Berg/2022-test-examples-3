import React from 'react';
import { render } from '@testing-library/react';

import { VideoPreview } from './VideoPreview';

const videoTitle = 'Заполнение характеристик';
const videoSrc = 'https://www.youtube.com/embed/Ol63f-qwa7k';

describe('VideoPreview', () => {
  test('render', () => {
    const app = render(<VideoPreview title={videoTitle} preview={videoSrc} src={videoSrc} />);

    app.getByText(videoTitle);
  });
});
