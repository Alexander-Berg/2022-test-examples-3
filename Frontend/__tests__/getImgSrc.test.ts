import { getImgSrc } from 'news/lib/helpers/getImgSrc';
import { getAvatarsHost } from 'neo/lib/getStaticsHost';

test('basic', () => {
  const actualPicture = getImgSrc({
    picture: {
      url: 'https://awesome.sex/boobs.png',
      groupId: 69,
      width: 1024,
      height: 768,
    },
    size: {
      width: 640,
      height: 480,
    },
  });
  const desiredPicture = {
    src: `https://${getAvatarsHost()}/get-ynews/69/c67a8b1fb861ecde5567ed51315297b4/640x480`,
    srcSet: undefined,
  };

  expect(actualPicture).toMatchObject(desiredPicture);
});

test('big resolution', () => {
  const actualPicture = getImgSrc({
    picture: {
      url: 'https://awesome.sex/boobs.png',
      groupId: 69,
      width: 2048,
      height: 1024,
    },
    size: {
      width: 640,
      height: 480,
    },
  });
  const desiredPicture = {
    src: `https://${getAvatarsHost()}/get-ynews/69/c67a8b1fb861ecde5567ed51315297b4/640x480`,
    srcSet: `https://${getAvatarsHost()}/get-ynews/69/c67a8b1fb861ecde5567ed51315297b4/1280x960 2x`,
  };

  expect(actualPicture).toMatchObject(desiredPicture);
});
