/* eslint-disable @typescript-eslint/ban-ts-comment */
import { Core, Model, prepareModels, HiddenParam } from '../index';

declare function m1(params: string): Promise<any>;
declare function m2(params: void): Promise<any>;
declare function m3(): Promise<any>;
declare class M1 extends Model<string, any> {
    action(params: string, core: any): Promise<any>
}
declare class M2 extends Model<void, any> {
    action(params: void): Promise<any>
}
declare class M3 extends Model<void, any> {
    action(): Promise<any>
}
declare class P4 {
    login: string;
    password?: HiddenParam<string>;
}
declare class M4 extends Model<P4, any> {
    action(params: P4): Promise<any>
}

export const models = prepareModels({ m1, m2, m3, M1, M2, M3, M4 } as const);
declare class TestCore extends Core<typeof models, any> {}
declare const core: TestCore;

core.request('m1', 'string');
// @ts-expect-error
core.request('m1');
// @ts-expect-error
core.request('m1', 13);

core.request('m2');
core.request('m2', undefined);
// @ts-expect-error
core.request('m2', 13);

core.request('m3');
core.request('m3', undefined);
// @ts-expect-error
core.request('m3', 13);

core.request('M1', 'string');
// @ts-expect-error
core.request('M1');
// @ts-expect-error
core.request('M1', 13);

core.request('M2');
core.request('M2', undefined);
// @ts-expect-error
core.request('M2', 13);

core.request('M3');
core.request('M3', undefined);
core.request('M3', 13);

core.request('M4', { login: 'a' });
core.request('M4', { login: 'a', password: 'p' });
core.request('M4', { login: 'a', password: new HiddenParam('p') });
