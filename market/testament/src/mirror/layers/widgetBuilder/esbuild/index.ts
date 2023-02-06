import {EsbuildWorker} from './worker';
import {WidgetBuilderLayer} from '../index';
import {resolveScriptPath} from '../../../../utils/relativePath';
import AbstractWidgetBuilderLayer from '../abstract';

export default class EsbuildWidgetLayer
    // eslint-disable-next-line @typescript-eslint/ban-types
    extends AbstractWidgetBuilderLayer<{}, EsbuildWorker>
    implements WidgetBuilderLayer
{
    static readonly ID = 'esbuild';

    constructor() {
        super(
            EsbuildWidgetLayer.ID,
            resolveScriptPath(__filename, './worker.js'),
        );
    }
}
