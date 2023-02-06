import { getUTMFromUrl } from '../LcJobsSessionInit.utils';
import { UTM_RECORD } from '../LcJobsSessionInit.types';

const oldWindowLocation = window.location;

const utmSourceMock = 'utm_source_mock_value';
const utmMediumMock = 'utm_medium_mock_value';
const utmCampaignMock = 'utm_campaign_mock_value';
const utmTermMock = 'utm_term_mock_value';
const utmContentMock = 'utm_content_mock_value';

const utmRecordMock: UTM_RECORD = {
    utm_campaign: utmCampaignMock,
    utm_content: utmContentMock,
    utm_medium: utmMediumMock,
    utm_source: utmSourceMock,
    utm_term: utmTermMock,
};

describe('LcJobsSessionInit.utils', () => {
    describe('getUTMFromUrl', () => {
        beforeEach(() => {
            delete window.location;
        });

        afterEach(() => {
            delete window.location;
            window.location = oldWindowLocation;
        });

        it('should return null when url doesn`t contain utm params', () => {
            // @ts-ignore
            window.location = new URL('https://www.example.com?some_param=123');
            expect(getUTMFromUrl()).toEqual(null);
        });

        it('should return object with key value of utm', () => {
            // @ts-ignore
            window.location = new URL(
                `https://www.example.com?utm_source=${utmSourceMock}&utm_medium=${utmMediumMock}&utm_campaign=${utmCampaignMock}&utm_term=${utmTermMock}&utm_content=${utmContentMock}`
            );

            expect(getUTMFromUrl()).toEqual(utmRecordMock);
        });

        it('should get from url only first value of utm param', () => {
            // @ts-ignore
            window.location = new URL(
                `https://www.example.com?utm_source=${utmSourceMock}&utm_source=${utmSourceMock}&utm_medium=${utmMediumMock}&utm_campaign=${utmCampaignMock}&utm_campaign=${utmCampaignMock}&utm_term=${utmTermMock}&utm_content=${utmContentMock}`
            );

            expect(getUTMFromUrl()).toEqual(utmRecordMock);
        });
    });
});
