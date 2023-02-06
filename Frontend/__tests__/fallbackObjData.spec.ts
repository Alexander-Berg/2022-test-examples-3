import { fallbackObjData } from '../fallbackObjData';

import moderationObject from '../../../../jest/__fixtures__/moderation_object.json';

let obj;

beforeEach(() => {
    obj = moderationObject;
});

describe('hacks/backend/fallbackObjData', () => {
    it('fallbacks banner history', () => {
        expect(fallbackObjData(obj)).toEqual(obj);
    });
    it('fallbacks display_href_sm history', () => {
        obj.type = 'display_href_sm';
        obj.data.text = 'text';

        expect(fallbackObjData(obj)).toEqual({ ...obj, data: { ...obj.data, display_href: obj.data.text } });
    });
    it('fallbacks sitelinks_set_sm history', () => {
        obj.type = 'sitelinks_set_sm';
        obj.data.links = [];

        expect(fallbackObjData(obj)).toEqual({ ...obj, data: { ...obj.data, sitelinks: obj.data.links } });
    });
    it('fallbacks content_promotion_video history', () => {
        obj.type = 'content_promotion_video';
        obj.data.video_hosting_url = 'https://yandex.ru/efir?stream_id=12345';

        expect(fallbackObjData(obj)).toEqual({ ...obj, data: { ...obj.data, video_hosting_url: 'https://frontend.vh.yandex.ru/player/12345' } });
    });
    it('fallbacks asset_vcard history', () => {
        obj.type = 'asset_vcard';
        obj.data.contact_email = 'contact_email';
        obj.data.worktime = 'worktime';

        expect(fallbackObjData(obj)).toEqual({ ...obj, data: { ...obj.data,
            email: obj.data.contact_email,
            work_time: obj.data.worktime,
        } });
    });
});
