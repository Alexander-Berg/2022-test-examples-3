import type {ExtendedPartnerFeedDTO, ExtendedBusinessFeedUploadHistoryResponse} from 'shared/bcm/feedProcessor/types';

const partnerFeedUpdates: ExtendedPartnerFeedDTO[] = [
    {
        isUploadedByBusiness: true,
        partnerId: 1234,
        feedId: 200842373,
        fileName: '10783379_6-53',
        feedType: 'ASSORTMENT_FEED',
        uploadedDate: '2022-01-24T09:53:35+03:00',
        statistics: {
            totalOffers: 3,
            declinedOffers: 0,
        },
        partnerMeta: null,
        historyFeedFileId: '0cc7cc81e187c1de300bbaf7ab9258765a4f4116150403b816b575341ba3d2d6',
        isUpload: true,
        uploadType: 'UPLOAD',
        status: 'PROCESSED_WITHOUT_ERRORS',
    },
];

export const fromGetBusinessFeedUploadHistoryResponse: ExtendedBusinessFeedUploadHistoryResponse = {
    partnerFeedUpdates,
    pager: {
        size: 10,
        previousPageToken: '',
        currentPageToken: '',
        nextPageToken: '-1234',
    },
};
