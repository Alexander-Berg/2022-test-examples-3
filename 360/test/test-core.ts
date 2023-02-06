/* eslint-disable @typescript-eslint/ban-ts-comment */
import { Core, Model, prepareModels } from '../index';

type ListOfServices = {
    [service: string]: (...args: any) => any;
}

declare function m1(params: string, core: AppCore): Promise<string>;
declare function m2(params: number, core: AppCore): Promise<number>;
declare class M3 extends Model<string, boolean, AppCore> {
    action(p: string, core: AppCore): Promise<boolean>;
}

const models = prepareModels({ m1, m2, M3 } as const);
declare const services: ListOfServices;

declare type AppCore = Core<typeof models, typeof services>;

declare const core: Core<typeof models, typeof services>;

export const a1 = core.request('m1', 'wow');
export const a2 = core.request('m2', 42);
export const a3 = core.request('M3', 'str');
