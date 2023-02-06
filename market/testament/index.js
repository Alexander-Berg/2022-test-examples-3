// @flow
// flowlint-next-line untyped-import: off
import Mirror from '@yandex-market/testament/mirror';
export {makeMirrorDesktop as makeMirror} from '@self/root/src/helpers/testament/mirror';

export function initGarsons(mirror: Mirror) {
    return mirror.getLayer('jest').runCode(() => {
        // $FlowFixMe
        require('@self/project/src/legacy/modules/DataCollector').register(
            // $FlowFixMe
            require('@self/platform/app/modules/DataCollector/garsons'),
            // $FlowFixMe
            require('@self/platform/app/modules/DataCollector/completers')
        );
    }, []);
}
