import {initLazyWidgets} from '@self/project/src/spec/gemini/helpers/initLazyWidgets';

export default {
    suiteName: 'AgitationTasksScrollUp',
    selector: '[data-zone-name="AgitationTasks"]',
    capture(actions) {
        initLazyWidgets(actions, 1000);
        // eslint-disable-next-line no-new-func
        actions.executeJS(new Function(`
            document.querySelector('[data-zone-name="Footer"]').scrollIntoView()
        `));
        actions.waitForElementToShow('[data-zone-name="Footer"]', 100);
        // eslint-disable-next-line no-new-func
        actions.executeJS(new Function(`
            window.scrollTo({
            top: 0,
            behavior: 'smooth',
        })
        `));
        // таймаут, чтобы не было бы видно полосу прокрутки
        actions.wait(2000);
    },
};
