import { checkOptions, AllOptions, AllOptionsKeys, Arguments } from './args';
declare type ArgvOptions<T extends AllOptionsKeys> = {
    usage: string;
    options?: T[];
    check?: typeof checkOptions;
};
export declare const parseArgv: <T extends "config" | "statface" | "verbose" | "command" | "debug" | "args">(argvOptions: ArgvOptions<T>) => Promise<Pick<Arguments<Pick<AllOptions, "config" | T>>, "_" | "$0" | Exclude<T, "config">>>;
export {};
