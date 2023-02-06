const MARKET_TESTING_SUPPLIER_ID = 10264169;

const MARSHRUT_WAREHOUSE_ID = 145;

export const PUBLISHING_OFFERS = {
  OFFER1: {
    ssku: '00065.00001.uiuiii',
    supplierId: MARKET_TESTING_SUPPLIER_ID,
    warehouseId: MARSHRUT_WAREHOUSE_ID,
    expectedData: {
      base: {
        active: false,
      },
      changed: {
        active: true,
      },
    },
  },
  OFFER2: {
    ssku: '00065.00026.100126173321',
    supplierId: MARKET_TESTING_SUPPLIER_ID,
    warehouseId: MARSHRUT_WAREHOUSE_ID,
    expectedData: {
      base: {
        active: true,
        basePrice: 8999,
        price: 6501,
      },
      changed: {
        active: false,
      },
    },
  },
  OFFER3: {
    ssku: '00065.00026.100126173325',
    supplierId: MARKET_TESTING_SUPPLIER_ID,
    warehouseId: MARSHRUT_WAREHOUSE_ID,
    expectedData: {
      base: {
        active: true,
        basePrice: 13001,
        price: 4441,
      },
      changed: {
        active: true,
        basePrice: 13001,
        price: 4331,
      },
    },
  },
  OFFER4: {
    ssku: '00065.00026.100126173327',
    supplierId: MARKET_TESTING_SUPPLIER_ID,
    warehouseId: MARSHRUT_WAREHOUSE_ID,
    expectedData: {
      base: {
        active: false,
      },
      changed: {
        active: true,
        basePrice: 9801,
        price: 6501,
      },
    },
  },
  OFFER5: {
    ssku: '00065.00026.100126173329',
    supplierId: MARKET_TESTING_SUPPLIER_ID,
    warehouseId: MARSHRUT_WAREHOUSE_ID,
    expectedData: {
      base: {
        active: false,
      },
      changed: {
        active: true,
        basePrice: 4001,
        price: 3401,
      },
    },
  },
  OFFER6: {
    ssku: '00065.00026.100126174049',
    supplierId: MARKET_TESTING_SUPPLIER_ID,
    warehouseId: MARSHRUT_WAREHOUSE_ID,
    expectedData: {
      base: {
        active: true,
        basePrice: 6500,
        price: 1234,
      },
      changed: {
        active: true,
        basePrice: 6512,
        price: 123,
      },
    },
  },
  OFFER7: {
    ssku: '00065.00026.100126174441',
    supplierId: MARKET_TESTING_SUPPLIER_ID,
    warehouseId: MARSHRUT_WAREHOUSE_ID,
    expectedData: {
      base: {
        active: true,
        basePrice: 7101,
        price: 6001,
      },
      changed: {
        active: true,
        basePrice: 7500,
        price: 6001,
      },
    },
  },
  OFFER8: {
    ssku: '00065.00026.100126174453',
    supplierId: MARKET_TESTING_SUPPLIER_ID,
    warehouseId: MARSHRUT_WAREHOUSE_ID,
    expectedData: {
      base: {
        active: true,
        basePrice: 6123,
        price: 5001,
      },
      changed: {
        active: false,
      },
    }
  },
  OFFER9: {
    ssku: '00065.00026.100126174429',
    supplierId: MARKET_TESTING_SUPPLIER_ID,
    warehouseId: MARSHRUT_WAREHOUSE_ID,
    expectedData: {
      base: {
        active: false,
      },
      changed: {
        active: true,
      }
    },
  },
  OFFER10: {
    ssku: '00065.00026.100126174433',
    supplierId: MARKET_TESTING_SUPPLIER_ID,
    warehouseId: MARSHRUT_WAREHOUSE_ID,
    expectedData: {
      base: {
        active: true,
      },
      changed: {
        active: false,
      }
    },
  },
  OFFER11: {
    ssku: '00065.00026.100126173331',
    supplierId: MARKET_TESTING_SUPPLIER_ID,
    warehouseId: MARSHRUT_WAREHOUSE_ID,
    expectedData: {
      base: {
        active: false,
      },
      changed: {
        active: true,
      }
    },
  },
  OFFER12: {
    ssku: '00065.00026.100126173333',
    supplierId: MARKET_TESTING_SUPPLIER_ID,
    warehouseId: MARSHRUT_WAREHOUSE_ID,
    expectedData: {
      base: {
        active: true,
      },
      changed: {
        active: false,
      }
    },
  },
};

const CLEAN_OFFER = {
  participation: false,
  fixedBasePrice: '',
  fixedPrice: '',
  errors: [],
  publishButtonEnabledByBackend: false,
};

export const SAVING_OFFERS = {
  REQUIRED_PARAMS_OFFER_1: {
    ssku: '00065.00026.100126173349',
    base: CLEAN_OFFER,
    changed: {
      participation: true,
      fixedBasePrice: '',
      fixedPrice: '',
      errors: [
        'Не указаны все необходимые цены для участия в акции',
      ],
      publishButtonEnabledByBackend: false,
    },
  },
  REQUIRED_PARAMS_OFFER_2: {
    ssku: '00065.00026.100126173349',
    base: CLEAN_OFFER,
    changed: {
      participation: true,
      fixedBasePrice: '1000',
      fixedPrice: '',
      errors: [
        'Не указаны все необходимые цены для участия в акции',
      ],
      publishButtonEnabledByBackend: false,
    },
  },
  REQUIRED_PARAMS_OFFER_3: {
    ssku: '00065.00026.100126173349',
    base: CLEAN_OFFER,
    changed: {
      participation: true,
      fixedBasePrice: '',
      fixedPrice: '800',
      errors: [
        'Не указаны все необходимые цены для участия в акции',
      ],
      publishButtonEnabledByBackend: false,
    },
  },
  REQUIRED_PARAMS_OFFER_4: {
    ssku: '00065.00026.100126173349',
    base: CLEAN_OFFER,
    changed: {
      participation: true,
      fixedBasePrice: '1000',
      fixedPrice: '800',
      errors: [],
      publishButtonEnabledByBackend: true,
    },
  },
  MINIMAL_DISCOUNT_OFFER_1: {
    ssku: '00065.00023.786644432',
    base: CLEAN_OFFER,
    changed: {
      participation: true,
      fixedBasePrice: '1000',
      fixedPrice: '900',
      errors: [
        'Скидка меньше минимальной в категории'
      ],
      publishButtonEnabledByBackend: false,
    },
  },
  MINIMAL_DISCOUNT_OFFER_2: {
    ssku: '00065.00023.786644432',
    base: CLEAN_OFFER,
    changed: {
      participation: true,
      fixedBasePrice: '1000',
      fixedPrice: '800',
      errors: [],
      publishButtonEnabledByBackend: true,
    },
  },
  MINIMAL_PRICE_OFFER_1: {
    ssku: '00065.00016.124',
    base: CLEAN_OFFER,
    changed: {
      participation: true,
      fixedBasePrice: '1000',
      fixedPrice: '0',
      errors: [
        'Цена продажи по акции должна быть не менее 1р'
      ],
      publishButtonEnabledByBackend: false,
    },
  },
  MINIMAL_PRICE_OFFER_2: {
    ssku: '00065.00016.124',
    base: CLEAN_OFFER,
    changed: {
      participation: true,
      fixedBasePrice: '1000',
      fixedPrice: '1',
      errors: [],
      publishButtonEnabledByBackend: true,
    },
  }
};
