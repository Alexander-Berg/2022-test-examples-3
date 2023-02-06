'use strict';

const { factory } = require('./address-data.js');

let reply;

test('found full result', () => {
    const result = factory({ json: { reply } });
    expect(result).toMatchSnapshot();
});

test('nothing found', () => {
    delete reply.geoObject;
    const result = factory({ json: { reply } });
    expect(result).toMatchSnapshot();
});

test('coverage', () => {
    delete reply.geoObject[0].metadata;
    reply.geoObject[0].geometry[0].point.lat = 60;
    const result = factory({ json: { reply } });
    expect(result).toMatchSnapshot();
});

beforeEach(() => {
    // deep-clone для бедных =)
    reply = JSON.parse(JSON.stringify({
        metadata: [
            {
                '.yandex.maps.proto.search.search.RESPONSE_METADATA': {
                    request: {
                        text: 'Яндекс Деньги',
                        results: 1,
                        skip: 0,
                        boundedBy: {
                            lowerCorner: {
                                lon: 37.04029699,
                                lat: 55.31140517
                            },
                            upperCorner: {
                                lon: 38.20471123,
                                lat: 56.19005574
                            }
                        }
                    },
                    found: 1
                }
            }
        ],
        geoObject: [
            {
                metadata: [
                    {
                        '.yandex.maps.proto.search.masstransit_1x.GEO_OBJECT_METADATA': {
                            stop: [
                                {
                                    name: 'Павелецкая',
                                    point: {
                                        lon: 37.636331275,
                                        lat: 55.731537026
                                    }
                                },
                                {
                                    name: 'Павелецкая',
                                    point: {
                                        lon: 37.638957317,
                                        lat: 55.729797793
                                    }
                                }
                            ]
                        }
                    },
                    {
                        '.yandex.maps.proto.search.router.GEO_OBJECT_METADATA': {
                            router: [
                                {
                                    type: [
                                        'MASS_TRANSIT',
                                        'AUTO'
                                    ]
                                }
                            ]
                        }
                    }
                ],
                name: 'Яндекс.Деньги',
                description: 'Садовническая ул., 82, стр. 2, Москва, Россия',
                boundedBy: {
                    lowerCorner: {
                        lon: 37.63847616,
                        lat: 55.73262069
                    },
                    upperCorner: {
                        lon: 37.64668676,
                        lat: 55.73725332
                    }
                },
                geometry: [
                    {
                        point: {
                            lon: 37.642584,
                            lat: 55.734936
                        }
                    }
                ]
            }
        ]
    }));
});
