import { AxiosResponse } from 'axios'

import * as interfaces from '../../../interfaces'


export enum TopLevelEnum {
    NULL = "NULL",
    ONE = "ONE",
}

export namespace Alpha {
    export enum AlphaNestedEnum {
        N_NULL = "N_NULL",
        N_ONE = "N_ONE",
    }
    export interface IAlphaNestedMessageData {
    }

    export interface IAlphaNestedMessage extends IAlphaNestedMessageData {
        _response?: AxiosResponse,
        readonly _isEmpty?: boolean,
        readonly _cleared?: IAlphaNestedMessage,
    }
}

export namespace Alpha.AlphaNestedMessage {
    export enum AlphaNestedMessageNestedEnum {
        NN_NULL = "NN_NULL",
        NN_ONE = "NN_ONE",
    }
}

export namespace TestProtobufUtils {
    export interface IContainerData {
        key: string,
        value: string,
    }

    export interface IContainer extends IContainerData {
        _response?: AxiosResponse,
        readonly _isEmpty?: boolean,
        readonly _cleared?: IContainer,
    }
    export enum Type {
        UNDEFINED = "UNDEFINED",
        BASE = "BASE",
        CUSTOM = "CUSTOM",
    }
}

export interface IAlphaData {
}

export interface IAlpha extends IAlphaData {
    _response?: AxiosResponse,
    readonly _isEmpty?: boolean,
    readonly _cleared?: IAlpha,
}

export interface ITestDeepMergeData {
    s: interfaces.google.protobuf.IStringValue,
    b: interfaces.google.protobuf.IBoolValue,
    rsMarker: string[],
    nested: interfaces.martylib.test.ITestDeepMerge,
    m: { [key: string]: interfaces.martylib.test.ITestDeepMerge },
    rm: interfaces.martylib.test.ITestDeepMerge[],
}

export interface ITestDeepMerge extends ITestDeepMergeData {
    _response?: AxiosResponse,
    readonly _isEmpty?: boolean,
    readonly _cleared?: ITestDeepMerge,
}

export interface ITestProtobufUtilsData {
    type: interfaces.martylib.test.TestProtobufUtils.Type,
    disallowedType: interfaces.martylib.test.TestProtobufUtils.Type,
    id: string,
    disallowedId: string,
    container: interfaces.martylib.test.TestProtobufUtils.IContainer,
    disallowedContainer: interfaces.martylib.test.TestProtobufUtils.IContainer,
    repeatedContainer: interfaces.martylib.test.TestProtobufUtils.IContainer[],
    disallowedRepeatedContainer: interfaces.martylib.test.TestProtobufUtils.IContainer[],
    map: { [key: string]: interfaces.martylib.test.TestProtobufUtils.IContainer },
    disallowedMap: { [key: string]: interfaces.martylib.test.TestProtobufUtils.IContainer },
}

export interface ITestProtobufUtils extends ITestProtobufUtilsData {
    _response?: AxiosResponse,
    readonly _isEmpty?: boolean,
    readonly _cleared?: ITestProtobufUtils,
}
