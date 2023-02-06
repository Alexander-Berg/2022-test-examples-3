import {mergeDeepRight} from 'ramda';

import type {Offer} from '~/app/bcm/datacamp/Backend/types/offer';

export default (draft: Offer = {}): Offer =>
    mergeDeepRight<Offer, Offer>(
        {
            identifiers: {
                offerId: 'ref001',
                extra: {
                    classifierMagicId2: 'e2a9be72478888c30b3553cd305c4f3c',
                    classifierGoodId: '1e9f017a546ebfe74cd36a0495393b9a',
                    recentBusinessId: 11103660,
                },
                businessId: 11103660,
            },
            status: {
                version: {
                    offerVersion: {
                        counter: {
                            low: 1073743819,
                            high: 410772344,
                            unsigned: true,
                        },
                    },
                    ucDataVersion: {
                        counter: {
                            low: 1704,
                            high: 408857149,
                            unsigned: true,
                        },
                    },
                    actualContentVersion: {
                        counter: {
                            low: 1073741977,
                            high: 410216564,
                            unsigned: true,
                        },
                    },
                    masterDataVersion: {
                        counter: {
                            low: -1073739966,
                            high: 410649645,
                            unsigned: true,
                        },
                    },
                    originalPartnerDataVersion: {
                        counter: {
                            low: 1704,
                            high: 408857149,
                            unsigned: true,
                        },
                    },
                    directSearchSnippetModerationSubscriptionVersion: {
                        counter: {
                            low: -1073738218,
                            high: 408856815,
                            unsigned: true,
                        },
                    },
                },
            },
            content: {
                partner: {
                    original: {
                        name: {
                            meta: {
                                source: 4,
                                timestamp: {
                                    seconds: {
                                        low: 1640866257,
                                        high: 0,
                                        unsigned: false,
                                    },
                                    nanos: 17000000,
                                },
                            },
                            value: 'Ботинки Geox J944FB',
                        },
                        description: {
                            meta: {
                                source: 4,
                                timestamp: {
                                    seconds: {
                                        low: 1640866257,
                                        high: 0,
                                        unsigned: false,
                                    },
                                    nanos: 17000000,
                                },
                            },
                            value: 'Отличные детские ботиночки на зиму',
                        },
                        vendor: {
                            meta: {
                                source: 4,
                                timestamp: {
                                    seconds: {
                                        low: 1640866257,
                                        high: 0,
                                        unsigned: false,
                                    },
                                    nanos: 17000000,
                                },
                            },
                            value: 'Geox',
                        },
                        model: {
                            meta: {
                                source: 4,
                                timestamp: {
                                    seconds: {
                                        low: 1640866257,
                                        high: 0,
                                        unsigned: false,
                                    },
                                    nanos: 17000000,
                                },
                            },
                        },
                        vendorCode: {
                            meta: {
                                source: 4,
                                timestamp: {
                                    seconds: {
                                        low: 1640866257,
                                        high: 0,
                                        unsigned: false,
                                    },
                                    nanos: 17000000,
                                },
                            },
                        },
                        barcode: {
                            meta: {
                                source: 4,
                                timestamp: {
                                    seconds: {
                                        low: 1640866257,
                                        high: 0,
                                        unsigned: false,
                                    },
                                    nanos: 17000000,
                                },
                            },
                        },
                        adult: {
                            meta: {
                                source: 4,
                                timestamp: {
                                    seconds: {
                                        low: 1640866257,
                                        high: 0,
                                        unsigned: false,
                                    },
                                    nanos: 17000000,
                                },
                            },
                            flag: false,
                        },
                        expiry: {
                            meta: {
                                source: 4,
                                timestamp: {
                                    seconds: {
                                        low: 1640866257,
                                        high: 0,
                                        unsigned: false,
                                    },
                                    nanos: 17000000,
                                },
                            },
                            validityPeriod: {},
                        },
                        countryOfOrigin: {
                            meta: {
                                source: 4,
                                timestamp: {
                                    seconds: {
                                        low: 1640866257,
                                        high: 0,
                                        unsigned: false,
                                    },
                                    nanos: 17000000,
                                },
                            },
                        },
                        weight: {
                            meta: {
                                source: 4,
                                timestamp: {
                                    seconds: {
                                        low: 1640866257,
                                        high: 0,
                                        unsigned: false,
                                    },
                                    nanos: 17000000,
                                },
                            },
                            valueMg: {
                                low: 700000,
                                high: 0,
                                unsigned: true,
                            },
                        },
                        dimensions: {
                            meta: {
                                source: 4,
                                timestamp: {
                                    seconds: {
                                        low: 1640866257,
                                        high: 0,
                                        unsigned: false,
                                    },
                                    nanos: 17000000,
                                },
                            },
                            lengthMkm: {
                                low: 200000,
                                high: 0,
                                unsigned: true,
                            },
                            widthMkm: {
                                low: 200000,
                                high: 0,
                                unsigned: true,
                            },
                            heightMkm: {
                                low: 200000,
                                high: 0,
                                unsigned: true,
                            },
                        },
                        category: {
                            meta: {
                                source: 4,
                                timestamp: {
                                    seconds: {
                                        low: 1640866257,
                                        high: 0,
                                        unsigned: false,
                                    },
                                    nanos: 17000000,
                                },
                            },
                            id: {
                                low: 1124286976,
                                high: 0,
                                unsigned: true,
                            },
                            name: 'Ботинки для девочек',
                            parentId: {
                                low: 0,
                                high: 0,
                                unsigned: true,
                            },
                            pathCategoryIds: '1124286976',
                            pathCategoryNames: 'Ботинки для девочек',
                            businessId: 11103660,
                        },
                        groupName: {
                            meta: {
                                source: 4,
                                timestamp: {
                                    seconds: {
                                        low: 1640866257,
                                        high: 0,
                                        unsigned: false,
                                    },
                                    nanos: 17000000,
                                },
                            },
                        },
                        manufacturer: {
                            meta: {
                                source: 4,
                                timestamp: {
                                    seconds: {
                                        low: 1640866257,
                                        high: 0,
                                        unsigned: false,
                                    },
                                    nanos: 17000000,
                                },
                            },
                        },
                        certificates: {
                            meta: {
                                source: 5,
                                timestamp: {
                                    seconds: {
                                        low: 1635428388,
                                        high: 0,
                                        unsigned: false,
                                    },
                                    nanos: 456571000,
                                },
                                applier: 4,
                            },
                        },
                        cargoTypes: {
                            meta: {
                                source: 4,
                                timestamp: {
                                    seconds: {
                                        low: 1640866257,
                                        high: 0,
                                        unsigned: false,
                                    },
                                    nanos: 17000000,
                                },
                            },
                        },
                    },
                    actual: {
                        title: {
                            meta: {
                                timestamp: {
                                    seconds: {
                                        low: 1635428596,
                                        high: 0,
                                        unsigned: false,
                                    },
                                },
                                applier: 3,
                            },
                            value: 'Ботинки Geox J944FB',
                        },
                        description: {
                            meta: {
                                timestamp: {
                                    seconds: {
                                        low: 1635428596,
                                        high: 0,
                                        unsigned: false,
                                    },
                                },
                                applier: 3,
                            },
                            value: 'Отличные детские ботиночки на зиму',
                        },
                        offerParams: {
                            meta: {
                                timestamp: {
                                    seconds: {
                                        low: 1635428596,
                                        high: 0,
                                        unsigned: false,
                                    },
                                },
                                applier: 3,
                            },
                            param: [
                                {
                                    name: 'vendor',
                                    value: 'Geox',
                                },
                                {
                                    name: 'delivery_weight',
                                    unit: 'кг',
                                    value: '0.7',
                                },
                                {
                                    name: 'delivery_length',
                                    unit: 'см',
                                    value: '20',
                                },
                                {
                                    name: 'delivery_width',
                                    unit: 'см',
                                    value: '20',
                                },
                                {
                                    name: 'delivery_height',
                                    unit: 'см',
                                    value: '20',
                                },
                            ],
                        },
                        priceFrom: {
                            meta: {
                                timestamp: {
                                    seconds: {
                                        low: 1635428596,
                                        high: 0,
                                        unsigned: false,
                                    },
                                },
                                applier: 3,
                            },
                        },
                        adult: {
                            meta: {
                                timestamp: {
                                    seconds: {
                                        low: 1635428596,
                                        high: 0,
                                        unsigned: false,
                                    },
                                },
                                applier: 3,
                            },
                            flag: false,
                        },
                        age: {
                            meta: {
                                timestamp: {
                                    seconds: {
                                        low: 1635428596,
                                        high: 0,
                                        unsigned: false,
                                    },
                                },
                                applier: 3,
                            },
                        },
                        barcode: {
                            meta: {
                                timestamp: {
                                    seconds: {
                                        low: 1635428596,
                                        high: 0,
                                        unsigned: false,
                                    },
                                },
                                applier: 3,
                            },
                        },
                        expiry: {
                            meta: {
                                timestamp: {
                                    seconds: {
                                        low: 1635428596,
                                        high: 0,
                                        unsigned: false,
                                    },
                                },
                                applier: 3,
                            },
                            validityPeriod: {},
                        },
                        weight: {
                            meta: {
                                timestamp: {
                                    seconds: {
                                        low: 1635428596,
                                        high: 0,
                                        unsigned: false,
                                    },
                                },
                                applier: 3,
                            },
                            valueMg: {
                                low: 700000,
                                high: 0,
                                unsigned: true,
                            },
                        },
                        dimensions: {
                            meta: {
                                timestamp: {
                                    seconds: {
                                        low: 1635428596,
                                        high: 0,
                                        unsigned: false,
                                    },
                                },
                                applier: 3,
                            },
                            lengthMkm: {
                                low: 200000,
                                high: 0,
                                unsigned: true,
                            },
                            widthMkm: {
                                low: 200000,
                                high: 0,
                                unsigned: true,
                            },
                            heightMkm: {
                                low: 200000,
                                high: 0,
                                unsigned: true,
                            },
                        },
                        downloadable: {
                            meta: {
                                timestamp: {
                                    seconds: {
                                        low: 1635428596,
                                        high: 0,
                                        unsigned: false,
                                    },
                                },
                                applier: 3,
                            },
                            flag: false,
                        },
                        type: {
                            meta: {
                                timestamp: {
                                    seconds: {
                                        low: 1635428596,
                                        high: 0,
                                        unsigned: false,
                                    },
                                },
                                applier: 3,
                            },
                            value: 1,
                        },
                        sellerWarranty: {
                            meta: {
                                timestamp: {
                                    seconds: {
                                        low: 1635428596,
                                        high: 0,
                                        unsigned: false,
                                    },
                                },
                                applier: 3,
                            },
                            warrantyPeriod: {},
                        },
                        vendor: {
                            meta: {
                                timestamp: {
                                    seconds: {
                                        low: 1635428596,
                                        high: 0,
                                        unsigned: false,
                                    },
                                },
                                applier: 3,
                            },
                            value: 'Geox',
                        },
                        category: {
                            meta: {
                                timestamp: {
                                    seconds: {
                                        low: 1635428596,
                                        high: 0,
                                        unsigned: false,
                                    },
                                },
                                applier: 3,
                            },
                            id: {
                                low: 1124286976,
                                high: 0,
                                unsigned: true,
                            },
                            name: 'Ботинки для девочек',
                            parentId: {
                                low: 0,
                                high: 0,
                                unsigned: true,
                            },
                            pathCategoryIds: '1124286976',
                            pathCategoryNames: 'Ботинки для девочек',
                            businessId: 11103660,
                        },
                        vendorCode: {
                            meta: {
                                timestamp: {
                                    seconds: {
                                        low: 1635428596,
                                        high: 0,
                                        unsigned: false,
                                    },
                                },
                                applier: 3,
                            },
                        },
                        cargoTypes: {
                            meta: {
                                timestamp: {
                                    seconds: {
                                        low: 1635428596,
                                        high: 0,
                                        unsigned: false,
                                    },
                                },
                                applier: 3,
                            },
                        },
                    },
                    originalTerms: {
                        sellerWarranty: {
                            meta: {
                                source: 4,
                                timestamp: {
                                    seconds: {
                                        low: 1640866257,
                                        high: 0,
                                        unsigned: false,
                                    },
                                    nanos: 17000000,
                                },
                            },
                            warrantyPeriod: {},
                        },
                        boxCount: {
                            meta: {
                                source: 4,
                                timestamp: {
                                    seconds: {
                                        low: 1640866257,
                                        high: 0,
                                        unsigned: false,
                                    },
                                    nanos: 17000000,
                                },
                            },
                        },
                    },
                    marketSpecificContent: {
                        parameterValues: {
                            meta: {
                                source: 4,
                                timestamp: {
                                    seconds: {
                                        low: 1640866257,
                                        high: 0,
                                        unsigned: false,
                                    },
                                    nanos: 17000000,
                                },
                            },
                            parameterValues: [
                                {
                                    valueSource: 1,
                                    valueState: 1,
                                    paramId: {
                                        low: 14871214,
                                        high: 0,
                                        unsigned: false,
                                    },
                                    paramName: 'Цвет товара для карточки',
                                    value: {
                                        valueType: 2,
                                        optionId: {
                                            low: 15132340,
                                            high: 0,
                                            unsigned: false,
                                        },
                                        strValue: 'синий/красный',
                                    },
                                },
                                {
                                    valueSource: 1,
                                    valueState: 1,
                                    paramId: {
                                        low: 13887626,
                                        high: 0,
                                        unsigned: false,
                                    },
                                    paramName: 'Цвет товара для фильтра',
                                    value: {
                                        valueType: 2,
                                        optionId: {
                                            low: 13898977,
                                            high: 0,
                                            unsigned: false,
                                        },
                                        strValue: 'синий',
                                    },
                                },
                                {
                                    valueSource: 1,
                                    valueState: 1,
                                    paramId: {
                                        low: 13887626,
                                        high: 0,
                                        unsigned: false,
                                    },
                                    paramName: 'Цвет товара для фильтра',
                                    value: {
                                        valueType: 2,
                                        optionId: {
                                            low: 13891866,
                                            high: 0,
                                            unsigned: false,
                                        },
                                        strValue: 'красный',
                                    },
                                },
                            ],
                        },
                        processingResponse: {
                            meta: {
                                source: 7,
                                timestamp: {
                                    seconds: {
                                        low: 1642598550,
                                        high: 0,
                                        unsigned: false,
                                    },
                                    nanos: 335103000,
                                },
                            },
                        },
                        rating: {
                            meta: {
                                source: 4,
                                timestamp: {
                                    seconds: {
                                        low: 1640866257,
                                        high: 0,
                                        unsigned: false,
                                    },
                                    nanos: 17000000,
                                },
                            },
                            currentRating: 0,
                            expectedRating: 17,
                        },
                    },
                },
                market: {
                    meta: {
                        timestamp: {
                            seconds: {
                                low: 1643076577,
                                high: 0,
                                unsigned: false,
                            },
                        },
                        applier: 3,
                    },
                    enrichedOffer: {
                        categoryId: 7815035,
                        modelId: 217267811,
                        marketCategoryName: 'Ботинки для мальчиков',
                    },
                    dimensions: {
                        weight: 0,
                        length: 0,
                        width: 0,
                        height: 0,
                    },
                    marketCategory: 'Ботинки для мальчиков',
                    categoryId: 7815035,
                    vendorId: 8443897,
                    productName: 'Kakadu',
                    irData: {
                        classifierCategoryId: 0,
                        probability: 0,
                        matchedId: 217267811,
                        enrichType: 1,
                        skutchType: 3,
                        classifierConfidentTopPercision: 0,
                    },
                    marketSkuPublishedOnMarket: true,
                    marketSkuPublishedOnBlueMarket: true,
                    vendorName: 'Kakadu',
                    realUcVersion: {
                        counter: {
                            low: 1704,
                            high: 408857149,
                            unsigned: true,
                        },
                    },
                },
                binding: {
                    partner: {
                        meta: {
                            source: 5,
                            timestamp: {
                                seconds: {
                                    low: 1635428388,
                                    high: 0,
                                    unsigned: false,
                                },
                                nanos: 456571000,
                            },
                            applier: 4,
                        },
                        marketSkuId: {
                            low: 1654881791,
                            high: 23,
                            unsigned: false,
                        },
                    },
                    approved: {
                        meta: {
                            source: 7,
                            timestamp: {
                                seconds: {
                                    low: 1642598550,
                                    high: 0,
                                    unsigned: false,
                                },
                                nanos: 335103000,
                            },
                        },
                        marketCategoryId: 7815035,
                        marketModelId: {
                            low: 217267811,
                            high: 0,
                            unsigned: false,
                        },
                        marketSkuId: {
                            low: 1654881791,
                            high: 23,
                            unsigned: false,
                        },
                        marketCategoryName: 'Ботинки для мальчиков',
                        marketModelName: 'Kakadu 5086',
                        marketSkuName: 'Kakadu',
                    },
                    ucMapping: {
                        meta: {
                            timestamp: {
                                seconds: {
                                    low: 1640762644,
                                    high: 0,
                                    unsigned: false,
                                },
                            },
                            applier: 3,
                        },
                        marketCategoryId: 7815035,
                        marketModelId: {
                            low: 217267811,
                            high: 0,
                            unsigned: false,
                        },
                        marketSkuId: {
                            low: 1654881791,
                            high: 23,
                            unsigned: false,
                        },
                        marketCategoryName: 'Ботинки для мальчиков',
                        marketModelName: 'Kakadu 5086',
                        marketSkuName: 'Kakadu',
                    },
                    blueUcMapping: {
                        meta: {
                            timestamp: {
                                seconds: {
                                    low: 1635428490,
                                    high: 0,
                                    unsigned: false,
                                },
                            },
                            applier: 3,
                        },
                        marketSkuId: {
                            low: 1654881791,
                            high: 23,
                            unsigned: false,
                        },
                    },
                    mappingForUc: {
                        meta: {
                            source: 7,
                            timestamp: {
                                seconds: {
                                    low: 3700481,
                                    high: 0,
                                    unsigned: false,
                                },
                            },
                        },
                        marketCategoryId: 7815023,
                        marketSkuId: {
                            low: 1654881791,
                            high: 23,
                            unsigned: false,
                        },
                    },
                },
                status: {
                    result: {
                        meta: {
                            source: 8,
                            timestamp: {
                                seconds: {
                                    low: 1635428490,
                                    high: 0,
                                    unsigned: false,
                                },
                            },
                        },
                        cardStatus: 1,
                    },
                    contentSystemStatus: {
                        meta: {
                            source: 7,
                            timestamp: {
                                seconds: {
                                    low: 1642598550,
                                    high: 0,
                                    unsigned: false,
                                },
                                nanos: 335103000,
                            },
                        },
                        cpcState: 1,
                        cpaState: 8,
                        allowCategorySelection: false,
                        allowModelSelection: false,
                        allowModelCreateUpdate: false,
                        modelBarcodeRequired: false,
                        statusContentVersion: {
                            counter: {
                                low: 1073741977,
                                high: 410216564,
                                unsigned: true,
                            },
                        },
                        partnerMappingStatus: {
                            timestamp: {
                                seconds: {
                                    low: 1635428388,
                                    high: 0,
                                    unsigned: false,
                                },
                                nanos: 456571000,
                            },
                            skuMappingState: 2,
                            modelMappingState: 0,
                            categoryMappingState: 0,
                        },
                        skuMappingConfidence: 4,
                        categoryRestriction: {
                            type: 2,
                            allowedCategoryId: {
                                low: 7815023,
                                high: 0,
                                unsigned: false,
                            },
                        },
                    },
                },
            },
            pictures: {
                partner: {
                    original: {
                        meta: {
                            source: 4,
                            timestamp: {
                                seconds: {
                                    low: 1640866257,
                                    high: 0,
                                    unsigned: false,
                                },
                                nanos: 17000000,
                            },
                        },
                    },
                },
                market: {
                    meta: {
                        timestamp: {
                            seconds: {
                                low: 1635428492,
                                high: 0,
                                unsigned: false,
                            },
                        },
                    },
                    productPictures: [
                        {
                            original: {
                                url: 'https://avatars.mds.yandex.net/get-mpic/1353698/img_id6338921550648254683.jpeg/orig',
                                width: 701,
                                height: 491,
                                containerWidth: 701,
                                containerHeight: 491,
                            },
                            thumbnails: [
                                {
                                    url: 'https://avatars.mds.yandex.net/get-mpic/1353698/img_id6338921550648254683.jpeg/100x100',
                                    width: 100,
                                    height: 100,
                                    containerWidth: 100,
                                    containerHeight: 100,
                                },
                            ],
                        },
                    ],
                },
            },
            resolution: {
                bySource: [
                    {
                        meta: {
                            source: 12,
                            timestamp: {
                                seconds: {
                                    low: 1639609806,
                                    high: 0,
                                    unsigned: false,
                                },
                                nanos: 304000000,
                            },
                        },
                        verdict: [
                            {
                                results: [
                                    {
                                        isBanned: false,
                                        applications: [11],
                                    },
                                ],
                            },
                            {
                                results: [
                                    {
                                        isBanned: false,
                                        applications: [9],
                                    },
                                ],
                            },
                        ],
                    },
                    {
                        meta: {
                            source: 7,
                            timestamp: {
                                seconds: {
                                    low: 1640094058,
                                    high: 0,
                                    unsigned: false,
                                },
                                nanos: 674449000,
                            },
                        },
                    },
                    {
                        meta: {
                            source: 26,
                            timestamp: {
                                seconds: {
                                    low: 1640094058,
                                    high: 0,
                                    unsigned: false,
                                },
                                nanos: 674449000,
                            },
                        },
                    },
                ],
                mdmPartnerVerdict: {
                    meta: {
                        source: 12,
                        timestamp: {
                            seconds: {
                                low: 1639609807,
                                high: 0,
                                unsigned: false,
                            },
                            nanos: 479604000,
                        },
                    },
                },
            },
        },
        draft,
    ) as Offer;
