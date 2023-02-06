import Viewport from '@self/platform/spec/page-objects/levitan-gui/Viewport';
import GalleryUgcSlider from '@self/platform/components/Gallery/GalleryUgcSlider/__pageObject';
import GallerySlider from '@self/platform/components/Gallery/GallerySlider/__pageObject';

export default {
    suiteName: 'UGCMediaGallery',
    selector: GallerySlider.root,
    before(actions, find) {
        actions
            // Так как мы не можем импортировать из левитана стили здесь придется использовать
            // вот такой некрасивый селектор
            .click(find(`${GalleryUgcSlider.root} ${Viewport.root}>div>div>div:nth-child(2) img`))
            .waitForElementToShow(GallerySlider.root, 3000);
    },
    capture: {
        firstImage() {
        },
        secondImage(actions, find) {
            actions
                // Делаем так, потому что action.flick падает на текущей версии.
                // executeJS также не подходит, так как принимает функцию, которую
                // сериализует с помощью toString - это не позволит замкнуть в нее
                // наши селекторы. Вариант конструировать функцию из строки а потом
                // передавать её в executeJS для десериализации не выглядит хорошим
                .mouseDown(find(`${GallerySlider.root}`))
                .mouseMove(find(`${GallerySlider.root}`), {x: -200, y: 0})
                .mouseUp()
                .wait(500);
        },
    },
};
