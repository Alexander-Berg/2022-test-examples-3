import { loadImage } from 'src/components/ImageUploadButton/loadImage';

describe('loadImage', () => {
  it('works', async () => {
    const image = await loadImage(new File(['test'], 'test_img'), 'test_src');

    expect(image).toEqual({
      url: 'data:application/octet-stream;base64,dGVzdA==',
      type: 'LOCAL',
      source: 'test_src',
    });
  });
});
