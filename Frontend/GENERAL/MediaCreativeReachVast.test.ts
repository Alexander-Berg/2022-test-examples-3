import MediaCreativeReachVast from './MediaCreativeReachVast';
import {
    testMediaCreativeReachDataThemeInBanner,
    testMediaCreativeReachDataThemeTwoImagesInBanner,
    testMediaCreativeReachDataThemeTwoImagesInBannerEqual
} from '../testDataMediaCreativeReach';
import { constructorDataVideoType, mediaDataCreativeType } from '../../../types';

describe('MediaCreativeReachVast', () => {
    it('should return valid VAST theme in_banner (video banner on TOP)', () => {
        const bannerMeta: mediaDataCreativeType = testMediaCreativeReachDataThemeInBanner.rtb.data.dc_params.data_params['72057605278919971'];
        const constructorDataVideo: constructorDataVideoType | undefined = bannerMeta?.constructor_data?.Video;

        const vast = (new MediaCreativeReachVast(bannerMeta, constructorDataVideo)).getXMLString();

        expect(vast).toMatchSnapshot();
    });

    it('should return valid VAST theme two_images_in_banner', () => {
        const bannerMeta: mediaDataCreativeType = testMediaCreativeReachDataThemeTwoImagesInBanner.rtb.data.dc_params.data_params['72057605237749699'];
        const constructorDataVideo: constructorDataVideoType | undefined = bannerMeta?.constructor_data?.Video;

        const vast = (new MediaCreativeReachVast(bannerMeta, constructorDataVideo)).getXMLString();

        expect(vast).toMatchSnapshot();
    });

    it('should return valid VAST theme two_images_in_banner_equal', () => {
        const bannerMeta: mediaDataCreativeType = testMediaCreativeReachDataThemeTwoImagesInBannerEqual.rtb.data.dc_params.data_params['72057605266095698'];
        const constructorDataVideo: constructorDataVideoType | undefined = bannerMeta?.constructor_data?.Video;

        const vast = (new MediaCreativeReachVast(bannerMeta, constructorDataVideo)).getXMLString();

        expect(vast).toMatchSnapshot();
    });
});
