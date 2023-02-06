import {FEED_TYPE} from 'shared/bcm/feedProcessor/constants';
import type {ExtendedPartnerFeedDTO} from 'shared/bcm/feedProcessor/types';

export const feedWithoutErrors: ExtendedPartnerFeedDTO = {
    isUploadedByBusiness: true,
    partnerId: 0,
    partnerMeta: null,
    feedId: 123,
    fileName: '10783379_6-53',
    status: 'PROCESSED_WITHOUT_ERRORS',
    historyFeedFileId: '123e',
    isUpload: true,
    uploadedDate: '2022-01-24T09:53:39+03:00',
    feedType: FEED_TYPE.ASSORTMENT_FEED,
    uploadType: 'UPLOAD',
    statistics: {
        totalOffers: 1,
        declinedOffers: 0,
    },
};

export const feedWithErrors: ExtendedPartnerFeedDTO = {
    isUploadedByBusiness: false,
    partnerId: 1234,
    partnerMeta: {
        partnerName: 'name',
        placementTypes: ['DROPSHIP'],
        campaignId: 890,
        partnerId: 1234,
    },
    feedId: 456,
    fileName: '10783379_6-54',
    status: 'PROCESSED_WITH_ERRORS',
    historyFeedFileId: '456e',
    isUpload: false,
    uploadedDate: '2022-01-24T09:53:39+03:00',
    feedType: FEED_TYPE.STOCK_FEED,
    uploadType: 'URL',
    statistics: {
        totalOffers: 1,
        declinedOffers: 1,
    },
};

export const feedWithNotModified: ExtendedPartnerFeedDTO = {
    isUploadedByBusiness: true,
    partnerId: 0,
    partnerMeta: null,
    feedId: 789,
    fileName: '10783379_6-55',
    status: 'FEED_NOT_MODIFIED',
    historyFeedFileId: '',
    isUpload: false,
    uploadedDate: '2022-01-24T09:53:39+03:00',
    feedType: FEED_TYPE.PRICES_FEED,
    uploadType: 'URL',
    statistics: null,
};

export const feedWithFailedToDownload: ExtendedPartnerFeedDTO = {
    isUploadedByBusiness: true,
    partnerId: 0,
    partnerMeta: null,
    feedId: 999,
    fileName: '10783379_6-57',
    status: 'COULD_NOT_DOWNLOAD',
    historyFeedFileId: '',
    isUpload: false,
    uploadedDate: '2022-01-24T09:53:39+03:00',
    feedType: FEED_TYPE.ASSORTMENT_FEED,
    uploadType: 'UPLOAD',
    statistics: null,
};
