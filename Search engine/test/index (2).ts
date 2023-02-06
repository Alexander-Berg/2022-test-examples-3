import { AxiosResponse } from 'axios'


import * as interfaces from '../../../interfaces'
import * as messages from '../../../messages'



    export class Alpha implements interfaces.martylib.test.IAlpha {
        public static FromData(data: interfaces.martylib.test.IAlphaData , _response?: AxiosResponse): Alpha {
            if (data === undefined) {
                const object = new Alpha()
                object._response = _response
                return object
            }
            return new Alpha(
                _response,
            )
        }



        public static get jsonschemas(): any {
            return [
                {
                    fileMatch: ['*'],
                    schema: messages.martylib.test.Alpha.jsonschema,
                    uri: messages.martylib.test.Alpha.jsonschemaUri,
                },
                ...(
                    messages.martylib.test.Alpha.jsonschemaDeps.map((x: any) => ({
                        fileMatch: ['*'],
                        schema: x.jsonschema,
                        uri: x.jsonschemaUri,
                    }))
                ),
            ]
        }

        public static get jsonschemaUri(): any {
            return 'proto://martylib.test.Alpha'
        }

        public static get jsonschema(): any {
            return {
                type: 'object',
                properties: {
                },
            }
        }

        public static get jsonschemaDeps(): any {
            return [
            ]
        }

        public _response?: AxiosResponse

        public constructor (
            _response?: AxiosResponse,
        ) {
            this._response = _response
        }

        public get _isEmpty(): boolean {
            return true
        }

        public get _cleared(): interfaces.martylib.test.IAlpha {
            const cleared = {} as interfaces.martylib.test.IAlpha
            return cleared
        }
    }


export namespace Alpha {

    export class AlphaNestedMessage implements interfaces.martylib.test.Alpha.IAlphaNestedMessage {
        public static FromData(data: interfaces.martylib.test.Alpha.IAlphaNestedMessageData , _response?: AxiosResponse): AlphaNestedMessage {
            if (data === undefined) {
                const object = new AlphaNestedMessage()
                object._response = _response
                return object
            }
            return new AlphaNestedMessage(
                _response,
            )
        }



        public static get jsonschemas(): any {
            return [
                {
                    fileMatch: ['*'],
                    schema: messages.martylib.test.Alpha.AlphaNestedMessage.jsonschema,
                    uri: messages.martylib.test.Alpha.AlphaNestedMessage.jsonschemaUri,
                },
                ...(
                    messages.martylib.test.Alpha.AlphaNestedMessage.jsonschemaDeps.map((x: any) => ({
                        fileMatch: ['*'],
                        schema: x.jsonschema,
                        uri: x.jsonschemaUri,
                    }))
                ),
            ]
        }

        public static get jsonschemaUri(): any {
            return 'proto://martylib.test.Alpha.AlphaNestedMessage'
        }

        public static get jsonschema(): any {
            return {
                type: 'object',
                properties: {
                },
            }
        }

        public static get jsonschemaDeps(): any {
            return [
                    messages.martylib.test.Alpha
            ]
        }

        public _response?: AxiosResponse

        public constructor (
            _response?: AxiosResponse,
        ) {
            this._response = _response
        }

        public get _isEmpty(): boolean {
            return true
        }

        public get _cleared(): interfaces.martylib.test.Alpha.IAlphaNestedMessage {
            const cleared = {} as interfaces.martylib.test.Alpha.IAlphaNestedMessage
            return cleared
        }
    }
}



    export class TestDeepMerge implements interfaces.martylib.test.ITestDeepMerge {
        public static FromData(data: interfaces.martylib.test.ITestDeepMergeData , _response?: AxiosResponse): TestDeepMerge {
            if (data === undefined) {
                const object = new TestDeepMerge()
                object._response = _response
                return object
            }
            return new TestDeepMerge(
                messages.google.protobuf.StringValue.FromData(data.s, undefined),
                messages.google.protobuf.BoolValue.FromData(data.b, undefined),
                data.rsMarker,
                messages.martylib.test.TestDeepMerge.FromData(data.nested, undefined),
                ((d) => { const result = {} as { [key: string]: interfaces.martylib.test.ITestDeepMerge }; Object.keys(d).forEach(k => { result[k] = messages.martylib.test.TestDeepMerge.FromData(d[k], undefined) }); return result })(data.m || {}),
                (data.rm || []).map(x => messages.martylib.test.TestDeepMerge.FromData(x, undefined)),
                _response,
            )
        }

        public s: interfaces.google.protobuf.IStringValue
        public b: interfaces.google.protobuf.IBoolValue
        public rsMarker: string[]
        public nested: interfaces.martylib.test.ITestDeepMerge
        public m: { [key: string]: interfaces.martylib.test.ITestDeepMerge }
        public rm: interfaces.martylib.test.ITestDeepMerge[]


        public static get jsonschemas(): any {
            return [
                {
                    fileMatch: ['*'],
                    schema: messages.martylib.test.TestDeepMerge.jsonschema,
                    uri: messages.martylib.test.TestDeepMerge.jsonschemaUri,
                },
                ...(
                    messages.martylib.test.TestDeepMerge.jsonschemaDeps.map((x: any) => ({
                        fileMatch: ['*'],
                        schema: x.jsonschema,
                        uri: x.jsonschemaUri,
                    }))
                ),
            ]
        }

        public static get jsonschemaUri(): any {
            return 'proto://martylib.test.TestDeepMerge'
        }

        public static get jsonschema(): any {
            return {
                type: 'object',
                properties: {
                    s: {
                        '$ref': 'proto://google.protobuf.StringValue'
,

                    },
                    b: {
                        '$ref': 'proto://google.protobuf.BoolValue'
,

                    },
                    rsMarker: {
                type: 'array',
        items: {
                    type: 'string'

        },

                    },
                    nested: {
                        '$ref': 'proto://martylib.test.TestDeepMerge'
,

                    },
                    m: {
                type: 'object',
        patternProperties: {
            '^.*$': {
                            '$ref': 'proto://martylib.test.TestDeepMerge'

            },
        }

                    },
                    rm: {
                type: 'array',
        items: {
                    '$ref': 'proto://martylib.test.TestDeepMerge'

        },

                    },
                },
            }
        }

        public static get jsonschemaDeps(): any {
            return [
                    messages.google.protobuf.BoolValue,
                    messages.google.protobuf.StringValue,
            ]
        }

        public _response?: AxiosResponse

        public constructor (
            s: interfaces.google.protobuf.IStringValue = '',
            b: interfaces.google.protobuf.IBoolValue = false,
            rsMarker: string[] = [],
            nested: interfaces.martylib.test.ITestDeepMerge = (new messages.martylib.test.TestDeepMerge()),
            m: { [key: string]: interfaces.martylib.test.ITestDeepMerge } = {},
            rm: interfaces.martylib.test.ITestDeepMerge[] = [],
            _response?: AxiosResponse,
        ) {
            this.s = s
            this.b = b
            this.rsMarker = rsMarker
            this.nested = nested
            this.m = m
            this.rm = rm
            this._response = _response
        }

        public get _isEmpty(): boolean {
            if (!this.s._isEmpty) {
                return false
            }

            if (!this.b._isEmpty) {
                return false
            }

            if (this.rsMarker.length !== 0) {
                return false
            }

            if (!this.nested._isEmpty) {
                return false
            }

            if (Object.keys(this.m).length !== 0) {
                return false
            }

            if (this.rm.length !== 0) {
                return false
            }

            return true
        }

        public get _cleared(): interfaces.martylib.test.ITestDeepMerge {
            const cleared = {} as interfaces.martylib.test.ITestDeepMerge
            if (!this.s._isEmpty) {
                cleared.s = this.s._cleared!
            }

            if (!this.b._isEmpty) {
                cleared.b = this.b._cleared!
            }

            if (this.rsMarker.length !== 0) {
                cleared.rsMarker = this.rsMarker
            }

            if (!this.nested._isEmpty) {
                cleared.nested = this.nested._cleared!
            }

            if (Object.keys(this.m).length !== 0) {
                cleared.m = ((d) => {const result = {} as { [key: string]: interfaces.martylib.test.ITestDeepMerge }; Object.keys(d).forEach(k => result[k] = d[k]._cleared!); return result})(this.m)
            }

            if (this.rm.length !== 0) {
                cleared.rm = this.rm.map(x => x._cleared!)
            }

            return cleared
        }
    }



    export class TestProtobufUtils implements interfaces.martylib.test.ITestProtobufUtils {
        public static FromData(data: interfaces.martylib.test.ITestProtobufUtilsData , _response?: AxiosResponse): TestProtobufUtils {
            if (data === undefined) {
                const object = new TestProtobufUtils()
                object._response = _response
                return object
            }
            return new TestProtobufUtils(
                interfaces.martylib.test.TestProtobufUtils.Type[data.type],
                interfaces.martylib.test.TestProtobufUtils.Type[data.disallowedType],
                data.id,
                data.disallowedId,
                messages.martylib.test.TestProtobufUtils.Container.FromData(data.container, undefined),
                messages.martylib.test.TestProtobufUtils.Container.FromData(data.disallowedContainer, undefined),
                (data.repeatedContainer || []).map(x => messages.martylib.test.TestProtobufUtils.Container.FromData(x, undefined)),
                (data.disallowedRepeatedContainer || []).map(x => messages.martylib.test.TestProtobufUtils.Container.FromData(x, undefined)),
                ((d) => { const result = {} as { [key: string]: interfaces.martylib.test.TestProtobufUtils.IContainer }; Object.keys(d).forEach(k => { result[k] = messages.martylib.test.TestProtobufUtils.Container.FromData(d[k], undefined) }); return result })(data.map || {}),
                ((d) => { const result = {} as { [key: string]: interfaces.martylib.test.TestProtobufUtils.IContainer }; Object.keys(d).forEach(k => { result[k] = messages.martylib.test.TestProtobufUtils.Container.FromData(d[k], undefined) }); return result })(data.disallowedMap || {}),
                _response,
            )
        }

        public type: interfaces.martylib.test.TestProtobufUtils.Type
        public disallowedType: interfaces.martylib.test.TestProtobufUtils.Type
        public id: string
        public disallowedId: string
        public container: interfaces.martylib.test.TestProtobufUtils.IContainer
        public disallowedContainer: interfaces.martylib.test.TestProtobufUtils.IContainer
        public repeatedContainer: interfaces.martylib.test.TestProtobufUtils.IContainer[]
        public disallowedRepeatedContainer: interfaces.martylib.test.TestProtobufUtils.IContainer[]
        public map: { [key: string]: interfaces.martylib.test.TestProtobufUtils.IContainer }
        public disallowedMap: { [key: string]: interfaces.martylib.test.TestProtobufUtils.IContainer }


        public static get jsonschemas(): any {
            return [
                {
                    fileMatch: ['*'],
                    schema: messages.martylib.test.TestProtobufUtils.jsonschema,
                    uri: messages.martylib.test.TestProtobufUtils.jsonschemaUri,
                },
                ...(
                    messages.martylib.test.TestProtobufUtils.jsonschemaDeps.map((x: any) => ({
                        fileMatch: ['*'],
                        schema: x.jsonschema,
                        uri: x.jsonschemaUri,
                    }))
                ),
            ]
        }

        public static get jsonschemaUri(): any {
            return 'proto://martylib.test.TestProtobufUtils'
        }

        public static get jsonschema(): any {
            return {
                type: 'object',
                properties: {
                    type: {
                        enum: ['UNDEFINED', 'BASE', 'CUSTOM']
,

                    },
                    disallowedType: {
                        enum: ['UNDEFINED', 'BASE', 'CUSTOM']
,

                    },
                    id: {
                        type: 'string'
,

                    },
                    disallowedId: {
                        type: 'string'
,

                    },
                    container: {
                        '$ref': 'proto://martylib.test.TestProtobufUtils.Container'
,

                    },
                    disallowedContainer: {
                        '$ref': 'proto://martylib.test.TestProtobufUtils.Container'
,

                    },
                    repeatedContainer: {
                type: 'array',
        items: {
                    '$ref': 'proto://martylib.test.TestProtobufUtils.Container'

        },

                    },
                    disallowedRepeatedContainer: {
                type: 'array',
        items: {
                    '$ref': 'proto://martylib.test.TestProtobufUtils.Container'

        },

                    },
                    map: {
                type: 'object',
        patternProperties: {
            '^.*$': {
                            '$ref': 'proto://martylib.test.TestProtobufUtils.Container'

            },
        }

                    },
                    disallowedMap: {
                type: 'object',
        patternProperties: {
            '^.*$': {
                            '$ref': 'proto://martylib.test.TestProtobufUtils.Container'

            },
        }

                    },
                },
            }
        }

        public static get jsonschemaDeps(): any {
            return [
                    messages.martylib.test.TestProtobufUtils.Container
            ]
        }

        public _response?: AxiosResponse

        public constructor (
            type: interfaces.martylib.test.TestProtobufUtils.Type = interfaces.martylib.test.TestProtobufUtils.Type.UNDEFINED,
            disallowedType: interfaces.martylib.test.TestProtobufUtils.Type = interfaces.martylib.test.TestProtobufUtils.Type.UNDEFINED,
            id: string = '',
            disallowedId: string = '',
            container: interfaces.martylib.test.TestProtobufUtils.IContainer = (new messages.martylib.test.TestProtobufUtils.Container()),
            disallowedContainer: interfaces.martylib.test.TestProtobufUtils.IContainer = (new messages.martylib.test.TestProtobufUtils.Container()),
            repeatedContainer: interfaces.martylib.test.TestProtobufUtils.IContainer[] = [],
            disallowedRepeatedContainer: interfaces.martylib.test.TestProtobufUtils.IContainer[] = [],
            map: { [key: string]: interfaces.martylib.test.TestProtobufUtils.IContainer } = {},
            disallowedMap: { [key: string]: interfaces.martylib.test.TestProtobufUtils.IContainer } = {},
            _response?: AxiosResponse,
        ) {
            this.type = type
            this.disallowedType = disallowedType
            this.id = id
            this.disallowedId = disallowedId
            this.container = container
            this.disallowedContainer = disallowedContainer
            this.repeatedContainer = repeatedContainer
            this.disallowedRepeatedContainer = disallowedRepeatedContainer
            this.map = map
            this.disallowedMap = disallowedMap
            this._response = _response
        }

        public get _isEmpty(): boolean {
            if (this.type !== interfaces.martylib.test.TestProtobufUtils.Type.UNDEFINED) {
                return false
            }

            if (this.disallowedType !== interfaces.martylib.test.TestProtobufUtils.Type.UNDEFINED) {
                return false
            }

            if (this.id !== '') {
                return false
            }

            if (this.disallowedId !== '') {
                return false
            }

            if (!this.container._isEmpty) {
                return false
            }

            if (!this.disallowedContainer._isEmpty) {
                return false
            }

            if (this.repeatedContainer.length !== 0) {
                return false
            }

            if (this.disallowedRepeatedContainer.length !== 0) {
                return false
            }

            if (Object.keys(this.map).length !== 0) {
                return false
            }

            if (Object.keys(this.disallowedMap).length !== 0) {
                return false
            }

            return true
        }

        public get _cleared(): interfaces.martylib.test.ITestProtobufUtils {
            const cleared = {} as interfaces.martylib.test.ITestProtobufUtils
            if (this.type !== interfaces.martylib.test.TestProtobufUtils.Type.UNDEFINED) {
                cleared.type = this.type
            }

            if (this.disallowedType !== interfaces.martylib.test.TestProtobufUtils.Type.UNDEFINED) {
                cleared.disallowedType = this.disallowedType
            }

            if (this.id !== '') {
                cleared.id = this.id
            }

            if (this.disallowedId !== '') {
                cleared.disallowedId = this.disallowedId
            }

            if (!this.container._isEmpty) {
                cleared.container = this.container._cleared!
            }

            if (!this.disallowedContainer._isEmpty) {
                cleared.disallowedContainer = this.disallowedContainer._cleared!
            }

            if (this.repeatedContainer.length !== 0) {
                cleared.repeatedContainer = this.repeatedContainer.map(x => x._cleared!)
            }

            if (this.disallowedRepeatedContainer.length !== 0) {
                cleared.disallowedRepeatedContainer = this.disallowedRepeatedContainer.map(x => x._cleared!)
            }

            if (Object.keys(this.map).length !== 0) {
                cleared.map = ((d) => {const result = {} as { [key: string]: interfaces.martylib.test.TestProtobufUtils.IContainer }; Object.keys(d).forEach(k => result[k] = d[k]._cleared!); return result})(this.map)
            }

            if (Object.keys(this.disallowedMap).length !== 0) {
                cleared.disallowedMap = ((d) => {const result = {} as { [key: string]: interfaces.martylib.test.TestProtobufUtils.IContainer }; Object.keys(d).forEach(k => result[k] = d[k]._cleared!); return result})(this.disallowedMap)
            }

            return cleared
        }
    }


export namespace TestProtobufUtils {

    export class Container implements interfaces.martylib.test.TestProtobufUtils.IContainer {
        public static FromData(data: interfaces.martylib.test.TestProtobufUtils.IContainerData , _response?: AxiosResponse): Container {
            if (data === undefined) {
                const object = new Container()
                object._response = _response
                return object
            }
            return new Container(
                data.key,
                data.value,
                _response,
            )
        }

        public key: string
        public value: string


        public static get jsonschemas(): any {
            return [
                {
                    fileMatch: ['*'],
                    schema: messages.martylib.test.TestProtobufUtils.Container.jsonschema,
                    uri: messages.martylib.test.TestProtobufUtils.Container.jsonschemaUri,
                },
                ...(
                    messages.martylib.test.TestProtobufUtils.Container.jsonschemaDeps.map((x: any) => ({
                        fileMatch: ['*'],
                        schema: x.jsonschema,
                        uri: x.jsonschemaUri,
                    }))
                ),
            ]
        }

        public static get jsonschemaUri(): any {
            return 'proto://martylib.test.TestProtobufUtils.Container'
        }

        public static get jsonschema(): any {
            return {
                type: 'object',
                properties: {
                    key: {
                        type: 'string'
,

                    },
                    value: {
                        type: 'string'
,

                    },
                },
            }
        }

        public static get jsonschemaDeps(): any {
            return [
                    messages.martylib.test.TestProtobufUtils,
            ]
        }

        public _response?: AxiosResponse

        public constructor (
            key: string = '',
            value: string = '',
            _response?: AxiosResponse,
        ) {
            this.key = key
            this.value = value
            this._response = _response
        }

        public get _isEmpty(): boolean {
            if (this.key !== '') {
                return false
            }

            if (this.value !== '') {
                return false
            }

            return true
        }

        public get _cleared(): interfaces.martylib.test.TestProtobufUtils.IContainer {
            const cleared = {} as interfaces.martylib.test.TestProtobufUtils.IContainer
            if (this.key !== '') {
                cleared.key = this.key
            }

            if (this.value !== '') {
                cleared.value = this.value
            }

            return cleared
        }
    }
}
