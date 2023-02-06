import {
    testMediaCreativeReachDataThemeTwoImagesInBanner,
    testMediaCreativeReachDataThemeTwoImagesInBannerEqual,
} from '../testDataMediaCreativeReach';
import { constructorDataHtmlType } from '../../../types';
import MediaCreativeReachTwoImages from './MediaCreativeReachTwoImages';
import { trimHtmlDeep } from '../../../utils/trimHtml';

// отличается в принципе только положение div-блока: class="video-container"
describe('MediaCreativeReachTwoImages', () => {
    it('should return valid Html theme two_images_in_banner', () => {
        const bannerMeta = testMediaCreativeReachDataThemeTwoImagesInBanner.rtb.data.dc_params.data_params['72057605237749699'];
        const constructorDataHtml: constructorDataHtmlType = bannerMeta.constructor_data.Html;
        const clickUrls = [''];
        const vast = '<VAST version="2.0"</VAST>';

        const html = (new MediaCreativeReachTwoImages(constructorDataHtml, vast, clickUrls)).getHTMLString();

        expect(trimHtmlDeep(html)).toMatchSnapshot();
    });

    it('should return valid Html theme theme two_images_in_banner_equal', () => {
        const bannerMeta = testMediaCreativeReachDataThemeTwoImagesInBannerEqual.rtb.data.dc_params.data_params['72057605266095698'];
        const constructorDataHtml: constructorDataHtmlType = bannerMeta.constructor_data.Html;
        const clickUrls = [''];
        const vast = '<VAST version="2.0"</VAST>';

        const html = (new MediaCreativeReachTwoImages(constructorDataHtml, vast, clickUrls)).getHTMLString();

        expect(trimHtmlDeep(html)).toMatchSnapshot();
    });
});
