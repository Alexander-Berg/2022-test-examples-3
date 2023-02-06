import {Spec} from 'swagger-schema-official';

export const leftSpec: Spec = {
    "swagger": "2.0",
    "info": {
        "version": "mock",
        "title": "Market Partner Interface Backend - left scheme"
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
                responseC_field1: {
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
                        "in": "query",
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
                        "type": "string"
                    }
                ],
                "responses": {}
            }
        },
        "/path-gonna-be-removed": {
            "get": {
                "responses": {}
            },
            "post": {
                "responses": {}
            },
        },
        "/an-old-one": {
            "get": {
                "parameters": [],
                "responses": {
                    "200": {
                        "description": "Im forever 200 until deprecated"
                    }
                }
            }
        },
        "/new-required-parameter": {
            "get": {
                "parameters": [
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
                        "required": false,
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
                        "name": "old-name",
                        "in": "query",
                        "description": "Параметр такой-то",
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
                "parameters": [
                    {
                        "name": "to-be-removed",
                        "in": "query",
                        "description": "Параметр будет удалён",
                        "required": false,
                        "type": "string"
                    }
                ],
                "responses": {
                    "200": {
                        "description": "mock response"
                    }
                }
            }
        }
    }
};
