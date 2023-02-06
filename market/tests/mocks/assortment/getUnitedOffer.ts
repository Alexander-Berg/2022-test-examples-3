import {mergeDeepRight} from 'ramda';

import type {UnitedOffer} from '~/app/bcm/datacamp/Backend/types';

export default (draft: UnitedOffer = {}, shopId = 11103659): UnitedOffer =>
    mergeDeepRight<UnitedOffer, UnitedOffer>(
        {
            basic: {
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
            service: {
                [shopId]: {
                    identifiers: {
                        shopId,
                        feedId: 201000464,
                        warehouseId: 51449,
                        offerId: 'ref001',
                        extra: {
                            shopSku: 'ref001',
                            wareMd5: 'bzIzyQPxIJeILehWswkh8Q',
                            classifierMagicId2: 'e2a9be72478888c30b3553cd305c4f3c',
                            classifierGoodId: '2076544424a174190e6b2458ed102b45',
                            marketSkuId: {
                                low: 1654881791,
                                high: 23,
                                unsigned: false,
                            },
                            recentFeedId: 201000464,
                            recentWarehouseId: 51449,
                            recentBusinessId: 11103660,
                            clientId: 1352190526,
                        },
                        businessId: 11103660,
                        realFeedId: 201000464,
                    },
                    meta: {
                        rgb: 2,
                        tsCreated: {
                            seconds: {
                                low: 1635410791,
                                high: 0,
                                unsigned: false,
                            },
                            nanos: 519000000,
                        },
                        scope: 2,
                        tsFirstAdded: {
                            seconds: {
                                low: 1635410791,
                                high: 0,
                                unsigned: false,
                            },
                        },
                    },
                    techInfo: {},
                    status: {
                        publish: 1,
                        disabled: [
                            {
                                meta: {
                                    source: 4,
                                    timestamp: {
                                        seconds: {
                                            low: 1642522688,
                                            high: 0,
                                            unsigned: false,
                                        },
                                        nanos: 622000000,
                                    },
                                },
                                flag: false,
                            },
                            {
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
                                flag: false,
                            },
                            {
                                meta: {
                                    source: 8,
                                    timestamp: {
                                        seconds: {
                                            low: 1635428490,
                                            high: 0,
                                            unsigned: false,
                                        },
                                    },
                                    applier: 3,
                                },
                                flag: false,
                            },
                            {
                                meta: {
                                    source: 11,
                                    timestamp: {
                                        seconds: {
                                            low: 1635428717,
                                            high: 0,
                                            unsigned: false,
                                        },
                                        nanos: 621916000,
                                    },
                                },
                                flag: false,
                            },
                        ],
                        hasGone: {
                            meta: {
                                source: 8,
                                timestamp: {
                                    seconds: {
                                        low: 1643096039,
                                        high: 0,
                                        unsigned: false,
                                    },
                                },
                            },
                            flag: false,
                        },
                        publishByPartner: 1,
                        result: 1,
                        readyForPublication: {
                            meta: {
                                source: 8,
                                timestamp: {
                                    seconds: {
                                        low: 1635428493,
                                        high: 0,
                                        unsigned: false,
                                    },
                                },
                            },
                            value: 1,
                        },
                        publication: {
                            meta: {
                                source: 22,
                                timestamp: {
                                    seconds: {
                                        low: 1635443720,
                                        high: 0,
                                        unsigned: false,
                                    },
                                },
                            },
                            value: 1,
                            originalPartnerDataVersion: {
                                counter: {
                                    low: 1704,
                                    high: 408857149,
                                    unsigned: true,
                                },
                            },
                        },
                        version: {
                            offerVersion: {
                                counter: {
                                    low: -1073739653,
                                    high: 408852697,
                                    unsigned: true,
                                },
                            },
                            ucDataVersion: {
                                counter: {
                                    low: -1073739653,
                                    high: 408852697,
                                    unsigned: true,
                                },
                            },
                            actualContentVersion: {
                                counter: {
                                    low: -1073739653,
                                    high: 408852697,
                                    unsigned: true,
                                },
                            },
                            masterDataVersion: {
                                counter: {
                                    low: -1073739653,
                                    high: 408852697,
                                    unsigned: true,
                                },
                            },
                            originalPartnerDataVersion: {
                                counter: {
                                    low: 1216,
                                    high: 408852698,
                                    unsigned: true,
                                },
                            },
                            directSearchSnippetModerationSubscriptionVersion: {
                                counter: {
                                    low: -1073739653,
                                    high: 408852697,
                                    unsigned: true,
                                },
                            },
                        },
                        originalCpa: {
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
                            flag: true,
                        },
                        unitedCatalog: {
                            meta: {
                                source: 4,
                                timestamp: {
                                    seconds: {
                                        low: 1635410791,
                                        high: 0,
                                        unsigned: false,
                                    },
                                    nanos: 518000000,
                                },
                            },
                            flag: true,
                        },
                        fieldsPlacementVersion: {
                            meta: {
                                source: 4,
                                timestamp: {
                                    seconds: {
                                        low: 1640866257,
                                        high: 0,
                                        unsigned: false,
                                    },
                                    nanos: 19000000,
                                },
                            },
                            value: 1,
                        },
                    },
                    content: {
                        partner: {
                            original: {},
                            actual: {},
                        },
                        status: {
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
                                serviceOfferState: 8,
                            },
                        },
                    },
                    price: {
                        basic: {
                            meta: {
                                source: 4,
                                timestamp: {
                                    seconds: {
                                        low: 1642500367,
                                        high: 0,
                                        unsigned: false,
                                    },
                                    nanos: 894000000,
                                },
                            },
                            binaryPrice: {
                                price: {
                                    low: 15098112,
                                    high: 3,
                                    unsigned: true,
                                },
                            },
                            binaryOldprice: {
                                price: {
                                    low: -1474836480,
                                    high: 4,
                                    unsigned: true,
                                },
                            },
                            vat: 7,
                        },
                        enableAutoDiscounts: {
                            meta: {
                                source: 5,
                                timestamp: {
                                    seconds: {
                                        low: 1635410791,
                                        high: 0,
                                        unsigned: false,
                                    },
                                    nanos: 519000000,
                                },
                                applier: 4,
                            },
                            flag: true,
                        },
                        originalPriceFields: {
                            vat: {
                                meta: {
                                    source: 4,
                                    timestamp: {
                                        seconds: {
                                            low: 1642588716,
                                            high: 0,
                                            unsigned: false,
                                        },
                                        nanos: 544000000,
                                    },
                                },
                                value: 7,
                            },
                        },
                    },
                    partnerInfo: {
                        meta: {
                            timestamp: {
                                seconds: {
                                    low: 1642773838,
                                    high: 0,
                                    unsigned: false,
                                },
                            },
                            applier: 3,
                        },
                        priorityRegions: '213',
                        supplierName: 'Экспрессович',
                        supplierId: shopId,
                        supplierType: 3,
                        cpa: 4,
                        autobrokerEnabled: true,
                        isBlueOffer: true,
                        isFulfillment: false,
                        cpc: 4,
                        isDsbs: false,
                        isLavka: false,
                        isExpress: true,
                        isEda: false,
                        isIgnoreStocks: false,
                        programType: 3,
                        isPreproduction: false,
                        isDisabled: false,
                        hasWarehousing: true,
                    },
                    orderProperties: {
                        meta: {
                            source: 11,
                            timestamp: {
                                seconds: {
                                    low: 1635428717,
                                    high: 0,
                                    unsigned: false,
                                },
                                nanos: 621916000,
                            },
                        },
                        orderMethod: 2,
                    },
                    stockInfo: {
                        marketStocks: {
                            meta: {
                                source: 11,
                                timestamp: {
                                    seconds: {
                                        low: 1643042295,
                                        high: 0,
                                        unsigned: false,
                                    },
                                    nanos: 942325000,
                                },
                            },
                            count: {
                                low: 6428,
                                high: 0,
                                unsigned: false,
                            },
                        },
                        partnerStocks: {
                            meta: {
                                source: 4,
                                timestamp: {
                                    seconds: {
                                        low: 1643096039,
                                        high: 0,
                                        unsigned: false,
                                    },
                                    nanos: 540000000,
                                },
                            },
                            count: {
                                low: 6500,
                                high: 0,
                                unsigned: false,
                            },
                        },
                    },
                    resolution: {
                        bySource: [
                            {
                                meta: {
                                    source: 12,
                                    timestamp: {
                                        seconds: {
                                            low: 1642618826,
                                            high: 0,
                                            unsigned: false,
                                        },
                                        nanos: 681000000,
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
                                    source: 8,
                                    timestamp: {
                                        seconds: {
                                            low: 1635428490,
                                            high: 0,
                                            unsigned: false,
                                        },
                                    },
                                },
                            },
                        ],
                        mdmPartnerVerdict: {
                            meta: {
                                source: 12,
                                timestamp: {
                                    seconds: {
                                        low: 1642618827,
                                        high: 0,
                                        unsigned: false,
                                    },
                                    nanos: 232072000,
                                },
                            },
                        },
                    },
                },
            },
            actual: {
                [shopId]: {
                    warehouse: {
                        '51449': {
                            identifiers: {
                                shopId,
                                feedId: 201000464,
                                warehouseId: 51449,
                                offerId: 'ref001',
                                extra: {
                                    shopSku: 'ref001',
                                    wareMd5: 'bzIzyQPxIJeILehWswkh8Q',
                                    classifierMagicId2: 'e2a9be72478888c30b3553cd305c4f3c',
                                    classifierGoodId: '2076544424a174190e6b2458ed102b45',
                                    marketSkuId: {
                                        low: 1654881791,
                                        high: 23,
                                        unsigned: false,
                                    },
                                    recentFeedId: 201000464,
                                    recentWarehouseId: 51449,
                                    recentBusinessId: 11103660,
                                },
                                businessId: 11103660,
                            },
                            meta: {
                                rgb: 2,
                                tsCreated: {
                                    seconds: {
                                        low: 1635410791,
                                        high: 0,
                                        unsigned: false,
                                    },
                                    nanos: 519000000,
                                },
                                scope: 2,
                                tsFirstAdded: {
                                    seconds: {
                                        low: 1635410791,
                                        high: 0,
                                        unsigned: false,
                                    },
                                },
                                platforms: {
                                    '1': false,
                                    '2': true,
                                    '5': false,
                                    '6': false,
                                    '7': false,
                                    '8': false,
                                    '9': false,
                                    '10': false,
                                    '11': false,
                                    '12': false,
                                    '14': false,
                                    '15': false,
                                },
                            },
                            techInfo: {},
                            status: {
                                publish: 1,
                                disabled: [
                                    {
                                        meta: {
                                            source: 8,
                                            timestamp: {
                                                seconds: {
                                                    low: 1635428490,
                                                    high: 0,
                                                    unsigned: false,
                                                },
                                            },
                                            applier: 3,
                                        },
                                        flag: false,
                                    },
                                    {
                                        meta: {
                                            source: 11,
                                            timestamp: {
                                                seconds: {
                                                    low: 1635428717,
                                                    high: 0,
                                                    unsigned: false,
                                                },
                                                nanos: 621916000,
                                            },
                                        },
                                        flag: false,
                                    },
                                ],
                                hasGone: {
                                    meta: {
                                        source: 8,
                                        timestamp: {
                                            seconds: {
                                                low: 1643096039,
                                                high: 0,
                                                unsigned: false,
                                            },
                                        },
                                    },
                                    flag: false,
                                },
                                readyForPublication: {
                                    meta: {
                                        source: 8,
                                        timestamp: {
                                            seconds: {
                                                low: 1635428493,
                                                high: 0,
                                                unsigned: false,
                                            },
                                        },
                                    },
                                    value: 1,
                                },
                                publication: {
                                    meta: {
                                        source: 22,
                                        timestamp: {
                                            seconds: {
                                                low: 1635443720,
                                                high: 0,
                                                unsigned: false,
                                            },
                                        },
                                    },
                                    value: 1,
                                    originalPartnerDataVersion: {
                                        counter: {
                                            low: 1704,
                                            high: 408857149,
                                            unsigned: true,
                                        },
                                    },
                                },
                                version: {
                                    offerVersion: {
                                        counter: {
                                            low: -1073739653,
                                            high: 408852697,
                                            unsigned: true,
                                        },
                                    },
                                    ucDataVersion: {
                                        counter: {
                                            low: -1073739653,
                                            high: 408852697,
                                            unsigned: true,
                                        },
                                    },
                                    actualContentVersion: {
                                        counter: {
                                            low: -1073739653,
                                            high: 408852697,
                                            unsigned: true,
                                        },
                                    },
                                    masterDataVersion: {
                                        counter: {
                                            low: -1073739653,
                                            high: 408852697,
                                            unsigned: true,
                                        },
                                    },
                                    directSearchSnippetModerationSubscriptionVersion: {
                                        counter: {
                                            low: -1073739653,
                                            high: 408852697,
                                            unsigned: true,
                                        },
                                    },
                                },
                                fieldsPlacementVersion: {
                                    meta: {
                                        source: 4,
                                        timestamp: {
                                            seconds: {
                                                low: 1640866257,
                                                high: 0,
                                                unsigned: false,
                                            },
                                            nanos: 19000000,
                                        },
                                    },
                                    value: 1,
                                },
                            },
                            price: {
                                enableAutoDiscounts: {
                                    meta: {
                                        timestamp: {
                                            seconds: {
                                                low: 1635410791,
                                                high: 0,
                                                unsigned: false,
                                            },
                                            nanos: 519000000,
                                        },
                                    },
                                    flag: true,
                                },
                            },
                            partnerInfo: {
                                meta: {
                                    timestamp: {
                                        seconds: {
                                            low: 1642773838,
                                            high: 0,
                                            unsigned: false,
                                        },
                                    },
                                    applier: 3,
                                },
                                priorityRegions: '213',
                                supplierName: 'Экспрессович',
                                supplierId: shopId,
                                supplierType: 3,
                                cpa: 4,
                                autobrokerEnabled: true,
                                isBlueOffer: true,
                                isFulfillment: false,
                                cpc: 4,
                                isDsbs: false,
                                isLavka: false,
                                isExpress: true,
                                isEda: false,
                                isIgnoreStocks: false,
                            },
                            orderProperties: {
                                meta: {
                                    source: 11,
                                    timestamp: {
                                        seconds: {
                                            low: 1635428717,
                                            high: 0,
                                            unsigned: false,
                                        },
                                        nanos: 621916000,
                                    },
                                },
                                orderMethod: 2,
                            },
                            stockInfo: {
                                marketStocks: {
                                    meta: {
                                        source: 11,
                                        timestamp: {
                                            seconds: {
                                                low: 1643042295,
                                                high: 0,
                                                unsigned: false,
                                            },
                                            nanos: 942325000,
                                        },
                                    },
                                    count: {
                                        low: 6428,
                                        high: 0,
                                        unsigned: false,
                                    },
                                },
                                partnerStocks: {
                                    meta: {
                                        source: 4,
                                        timestamp: {
                                            seconds: {
                                                low: 1643096039,
                                                high: 0,
                                                unsigned: false,
                                            },
                                            nanos: 540000000,
                                        },
                                    },
                                    count: {
                                        low: 6500,
                                        high: 0,
                                        unsigned: false,
                                    },
                                },
                            },
                            resolution: {
                                bySource: [
                                    {
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
                                    },
                                ],
                            },
                        },
                    },
                },
            },
        },
        draft,
    ) as UnitedOffer;
