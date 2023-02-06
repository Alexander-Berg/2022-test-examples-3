import { thumbnailer } from '../../../../src/helpers/thumbnailer';

const testFile = new File([], 'file.png', { type: 'image/png' });

const imageWidth = 1280;
const imageHeight = 1024;
const previewSize = 150;

describe('src/helpers/thumbnailer', () => {
    const originalDocumentCreateElement = document.createElement;
    const originalWindowCreateImageBitmap = window.createImageBitmap;
    const originalWindowURL = window.URL;
    const originalWindowImage = window.Image;
    const drawImage = jest.fn();
    const canvasElementMock = {
        getContext: jest.fn(() => ({
            drawImage
        })),
        toDataURL: jest.fn()
    };
    beforeEach(() => {
        window.createImageBitmap = jest.fn();
        document.createElement = () => canvasElementMock;
        window.URL = {
            createObjectURL: jest.fn()
        };
        /**
         *
         */
        function mockedImage() {
            this.width = imageWidth;
            this.height = imageHeight;
            setTimeout(() => {
                this.onload();
            });
        }
        window.Image = mockedImage;
    });
    afterEach(() => {
        jest.clearAllMocks();
        document.createElement = originalDocumentCreateElement;
        window.createImageBitmap = originalWindowCreateImageBitmap;
        window.URL = originalWindowURL;
        window.Image = originalWindowImage;
    });

    it('should call window.createImageBitmap, canvas.getContext, canvasContext.drawImage and canvas.toDataURL', async() => {
        window.createImageBitmap.mockResolvedValue({ height: imageHeight, width: imageWidth });
        await thumbnailer(testFile, previewSize);
        expect(window.createImageBitmap).toBeCalled();
        expect(canvasElementMock.getContext).toBeCalled();
        expect(drawImage).toBeCalled();
        expect(canvasElementMock.toDataURL).toBeCalled();

        expect(popFnCalls(drawImage)).toMatchSnapshot();

        expect(window.URL.createObjectURL).not.toBeCalled();
    });

    it('should use fallbackThumbnailer if window.createImageBitmap rejects', async() => {
        window.createImageBitmap.mockRejectedValue();
        await thumbnailer(testFile, previewSize);
        expect(window.createImageBitmap).toBeCalled();
        expect(window.URL.createObjectURL).toBeCalled();
        expect(canvasElementMock.getContext).toBeCalled();
        expect(drawImage).toBeCalled();
        expect(canvasElementMock.toDataURL).toBeCalled();

        expect(popFnCalls(drawImage)).toMatchSnapshot();
    });

    it('should use fallbackThumbnailer if window.createImageBitmap does not exist', async() => {
        delete window.createImageBitmap;
        await thumbnailer(testFile, previewSize);
        expect(window.URL.createObjectURL).toBeCalled();
        expect(canvasElementMock.getContext).toBeCalled();
        expect(drawImage).toBeCalled();
        expect(canvasElementMock.toDataURL).toBeCalled();

        expect(popFnCalls(drawImage)).toMatchSnapshot();
    });
});
