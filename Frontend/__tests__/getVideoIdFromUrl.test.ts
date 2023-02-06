import { getVideoIdFromUrl } from '../getVideoIdFromUrl';

const possibleVideoUrls = [
  { videoUrl: 'https://frontend.vh.yandex.ru/player/vSgo3_Yx8mXY', videoId: 'vSgo3_Yx8mXY' },
  { videoUrl: 'https://frontend.vh.yandex.ru/player/vQWpY4XAzHW8', videoId: 'vQWpY4XAzHW8' },
  { videoUrl: 'https://frontend.vh.yandex.ru/player/v2dm4d-cymC3E', videoId: 'v2dm4d-cymC3E' },
];

describe('Корректно парсится URL видеохостинга: ', () => {
  possibleVideoUrls.forEach(({ videoUrl, videoId }, index) => {
    test(`(${index}) ${videoUrl}`, () => {
      const parsedVideoId = getVideoIdFromUrl(videoUrl);

      expect(parsedVideoId).toEqual(videoId);
    });
  });
});
