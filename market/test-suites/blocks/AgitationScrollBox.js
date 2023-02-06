import {initLazyWidgets} from '@self/project/src/spec/gemini/helpers/initLazyWidgets';

export default {
    suiteName: 'AgitationScrollBox',
    selector: '[data-zone-name="AgitationTasks"]',
    capture(actions) {
        initLazyWidgets(actions, 1000);
        // таймаут, чтобы не было бы видно полосу прокрутки
        actions.wait(2000);
    },
};
