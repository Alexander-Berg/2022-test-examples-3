import Viewport from '@self/platform/spec/page-objects/levitan-gui/Viewport';
import UgcMediaGallery from '@self/platform/widgets/content/UgcMediaGallery/__pageObject';
import GalleryModal from '@self/root/src/components/GalleryModalDesktop/__pageObject';

export default {
    suiteName: 'UGCMediaGallery',
    selector: GalleryModal.main,
    before(actions, find) {
        actions
            // Так как мы не можем импортировать из левитана стили здесь придется использовать
            // вот такой некрасивый селектор
            .click(find(`${UgcMediaGallery.root} ${Viewport.root}>div>div>div:nth-child(2) img`))
            .waitForElementToShow(GalleryModal.root, 3000);
    },
    capture: {
        firstImage() {},
        secondImage(actions, find) {
            actions
                .click(find(`${GalleryModal.root} ${GalleryModal.buttonNext}`))
                .wait(500);
        },
    },
};
