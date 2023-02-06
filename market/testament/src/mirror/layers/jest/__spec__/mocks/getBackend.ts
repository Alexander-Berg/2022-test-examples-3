import {JestWorker} from '../../worker';
import JestLayer from '../../index';

declare let getBackend: <TAPI>(id: string) => TAPI | null;

export default (): Promise<123> | void =>
    getBackend<JestWorker>(JestLayer.ID)?.runCode('()=>123', []);
