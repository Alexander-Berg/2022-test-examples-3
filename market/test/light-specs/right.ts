import {Spec} from 'swagger-schema-official';

export const rightSpec: Spec = {
    "swagger": "2.0",
    "info": {
        "version": "mock",
        "title": "Market Partner Interface Backend - right scheme"
    },
    definitions: {
        responseA: {
            type: 'object',
            properties: {
                responseA_propertyB: {
                    $ref: '#/definitions/responseB'
                }
            }
        },
        responseB: {
            type: 'object',
            properties: {
                responseB_propertyC: {
                    $ref: '#/definitions/responseC'
                }
            }
        },
        responseC: {
            type: 'object',
            properties: {
                cGetsRenamed: {
                    type: 'number'
                },
            }
        },
        Array: {
            type: 'array',
            items: {
                $ref: '#/definitions/ArrayItem'
            }
        },
        ArrayItem: {
            type: 'object',
            properties: {
                data: {
                    $ref: '#/definitions/responseC',
                }
            }
        }
    },
    "paths": {
        "/break-object-dto": {
            "get": {
                parameters: [],
                responses: {
                    '200': {
                        description: '200 ok',
                        schema: {
                            $ref: '#/definitions/responseA'
                        }
                    }
                }
            }
        },
        "/break-array-dto": {
            "get": {
                parameters: [],
                responses: {
                    '200': {
                        description: '200 ok',
                        schema: {
                            $ref: '#/definitions/Array'
                        }
                    }
                }
            }
        },
        "/parameter-gets-moved": {
            "get": {
                "parameters": [
                    {
                        "name": "whateverId",
                        "in": "body",
                        "description": "Параметр",
                        "required": false,
                        "type": "string"
                    }
                ],
                "responses": {}
            }
        },
        "/parameter-type-is-going-to-change": {
            "get": {
                "parameters": [
                    {
                        "name": "hyperId",
                        "in": "query",
                        "description": "Гипер параметр",
                        "required": false,
                        "type": "number"
                    }
                ],
                "responses": {}
            }
        },
        "/an-old-one": {
        },
        "/new-required-parameter": {
            "get": {
                "parameters": [
                    {
                        "name": "requiredParam",
                        "in": "query",
                        "description": "Параметр внезапно стал обязательным",
                        "required": true,
                        "type": "string"
                    }
                ],
                "responses": {
                    "200": {
                        "description": "mock response"
                    }
                }
            }
        },
        "/parameter-became-required": {
            "get": {
                "parameters": [
                    {
                        "name": "requiredParam",
                        "in": "query",
                        "description": "Параметр внезапно стал обязательным",
                        "required": true,
                        "type": "string"
                    }
                ],
                "responses": {
                    "200": {
                        "description": "mock response"
                    }
                }
            }
        },
        "/parameter-became-renamed": {
            "get": {
                "parameters": [
                    {
                        "name": "new-name-1",
                        "in": "query",
                        "description": "Параметр такой-то",
                        "required": true,
                        "type": "string"
                    },
                    {
                        "name": "new-name-2",
                        "in": "path",
                        "description": "Параметр такой-то",
                        "required": true,
                        "type": "string"
                    },
                    {
                        "name": "new-name-3",
                        "in": "query",
                        "description": "Параметр такой-то",
                        "required": false,
                        "type": "string"
                    },
                    {
                        "name": "new-name-4",
                        "in": "query",
                        "description": "Параметр такой-то, но другой",
                        "required": true,
                        "type": "string"
                    }
                ],
                "responses": {
                    "200": {
                        "description": "mock response"
                    }
                }
            }
        },
        "/parameter-was-removed": {
            "get": {
                "parameters": [],
                "responses": {
                    "200": {
                        "description": "mock response"
                    }
                }
            }
        }
    }
}
