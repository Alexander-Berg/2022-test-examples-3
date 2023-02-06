import * as React from 'react';
import { shallow } from 'enzyme';

import { VideoSandbox } from '../VideoSandbox';

const defaultData = {
    innerHtml: '<style>h1 { color: green; }</style><h1>DefaultText</h1>',
};
const additionalData = {
    className: 'mixed-class',
    width: 100,
    height: 50,
    iframeProperties: {
        allowfullscreen: true,
    },
};

// from https://github.com/facebook/react/blob/1034e26fe5e42ba07492a736da7bdf5bf2108bc6/packages/react-dom/src/server/escapeTextForBrowser.js#L49-L92
const matchHtmlRegExp = /["'&<>]/;

function escapeHtml(string) {
    const str = String(string);
    const match = matchHtmlRegExp.exec(str);

    if (!match) {
        return str;
    }

    let escape;
    let html = '';
    let index;
    let lastIndex = 0;

    for (index = match.index; index < str.length; index++) {
        switch (str.charCodeAt(index)) {
            case 34: // "
                escape = '&quot;';
                break;
            case 38: // &
                escape = '&amp;';
                break;
            case 39: // '
                escape = '&#x27;'; // modified from escape-html; used to be '&#39'
                break;
            case 60: // <
                escape = '&lt;';
                break;
            case 62: // >
                escape = '&gt;';
                break;
            default:
                continue;
        }

        if (lastIndex !== index) {
            html += str.substring(lastIndex, index);
        }

        lastIndex = index + 1;
        html += escape;
    }

    return lastIndex !== index ? html + str.substring(lastIndex, index) : html;
}

const iframeSrc = (innerHtml: string) =>
    `//yastatic.net/video-player/0xdb28055/pages-common/default/default.html#html=${escapeHtml(innerHtml)}`;

describe('VideoSandbox', () => {
    it('should render without crashing', () => {
        const wrapper = shallow(<VideoSandbox {...defaultData} />);

        expect(wrapper.length).toEqual(1);
    });

    it('should render correct default html', () => {
        const wrapper = shallow(<VideoSandbox {...defaultData} />);

        expect(wrapper.find('iframe').html()).toEqual(
            // tslint:disable-next-line:max-line-length
            `<iframe scrolling="no" frameBorder="0" sandbox=\"allow-forms allow-scripts allow-top-navigation allow-same-origin allow-presentation allow-popups allow-popups-to-escape-sandbox\" src="${iframeSrc(defaultData.innerHtml)}" style="width:auto;height:auto"></iframe>`
        );
    });

    it('should render correct class if provided', () => {
        const wrapper = shallow(<VideoSandbox {...defaultData} className={additionalData.className} />);
        expect(wrapper.find('iframe').hasClass('mixed-class')).toEqual(true);
    });

    it('should render correct width and height if provided', () => {
        const wrapper = shallow(<VideoSandbox {...defaultData} width={additionalData.width} height={additionalData.height} />);

        expect(wrapper.find('iframe').prop('style')).toEqual({ height: '50px', width: '100px' });
    });

    it('should render correct default iframe properties if provided', () => {
        const wrapper = shallow(<VideoSandbox {...defaultData} />);

        expect(wrapper.find('iframe').prop('scrolling')).toEqual('no');
    });

    it('should render correct iframeProperties if provided', () => {
        const wrapper = shallow(<VideoSandbox {...defaultData} iframeProperties={additionalData.iframeProperties} />);

        expect(wrapper.find('iframe').prop('allowfullscreen')).toEqual(true);
    });
});
