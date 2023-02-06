import FullscreenBanner from '../../../../../src/components/fullscreen-banner';

import React from 'react';
import { render } from 'enzyme';

const runTest = (props) => {
    const component = render(
        <FullscreenBanner {...props}/>
    );
    expect(component).toMatchSnapshot();
};

describe('awaps =>', () => {
    it('баннер', () => {
        runTest({
            src: 'https://banner-image',
            link: 'https://banner-link',
        });
    });

    it('баннер с пикселями статистики', () => {
        runTest({
            src: 'https://banner-image',
            link: 'https://banner-link',
            viewNotices: ['https://first-view-notice', 'https://second-view-notice'],
            winNotice: 'https://win-notice'
        });
    });
});
