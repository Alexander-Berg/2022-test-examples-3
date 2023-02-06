import { Options } from 'yargs';
declare type RecordBy<T, K> = {
    [P in keyof T]: K;
};
export declare type Arguments<T> = T & {
    /** Non-option arguments */
    _: string[];
    /** The script name or node command */
    $0: string;
};
export declare type BaseOptions = {};
export declare type ConfigOptions = BaseOptions & Partial<{}>;
export declare type CliArgv = {
    args: string[];
};
export declare type CliConfigOption = {
    config: string;
};
export declare type CliOptions = BaseOptions & CliConfigOption & {
    statface: string;
    verbose: boolean;
    command: string;
    debug: boolean;
};
export declare type AllOptions = CliArgv & CliOptions & ConfigOptions;
export declare const defaults: {
    config: string;
};
export declare const options: RecordBy<CliOptions, Options>;
export declare type AllOptionsKeys = keyof AllOptions;
export declare const getOptions: (optionKeys: "config" | ("config" | "statface" | "verbose" | "command" | "debug" | "args")[]) => Pick<RecordBy<CliOptions, Options>, "config" | "statface" | "verbose" | "command" | "debug">;
export declare const checkOptions: <T>(argv: T & {
    /** Non-option arguments */
    _: string[];
    /** The script name or node command */
    $0: string;
} & CliConfigOption) => any;
export {};
