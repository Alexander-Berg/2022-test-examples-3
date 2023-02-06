import {
    testMediaCreativeReachDataThemeInBanner,
    testMediaCreativeReachDataThemeInBannerInverse
} from '../testDataMediaCreativeReach';
import { constructorDataHtmlType } from '../../../types';
import MediaCreativeReachTopBottom from './MediaCreativeReachTopBottom';
import { trimHtmlDeep } from '../../../utils/trimHtml';

// отличается в принципе только положение div-блока: class="video-container"
describe('MediaCreativeReachTopBottom', () => {
    it('should return valid Html theme in_banner (video banner on TOP)', () => {
        const bannerMeta = testMediaCreativeReachDataThemeInBanner.rtb.data.dc_params.data_params['72057605278919971'];
        const constructorDataHtml: constructorDataHtmlType = bannerMeta.constructor_data.Html;
        const clickUrls = [''];
        const vast = '<VAST version="2.0"</VAST>';

        const html = (new MediaCreativeReachTopBottom(constructorDataHtml, vast, clickUrls)).getHTMLString();

        expect(trimHtmlDeep(html)).toMatchSnapshot();
    });

    it('should return valid Html theme in_banner (video banner on BOTTOM)', () => {
        const bannerMeta = testMediaCreativeReachDataThemeInBannerInverse.rtb.data.dc_params.data_params['72057605296578959'];
        const constructorDataHtml: constructorDataHtmlType = bannerMeta.constructor_data.Html;
        const clickUrls = [''];
        const vast = '<VAST version="2.0"</VAST>';

        const html = (new MediaCreativeReachTopBottom(constructorDataHtml, vast, clickUrls)).getHTMLString();

        expect(trimHtmlDeep(html)).toMatchSnapshot();
    });
});
