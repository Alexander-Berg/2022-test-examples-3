import { ProcessingConfigInfo } from 'src/java/definitions';

export const processingConfigInfo: ProcessingConfigInfo = {
  fileColumns: [
    {
      header: 'Категория',
      unique: false,
      sampleValues: ['Ножи кухонные'],
      hasEveryRow: false,
      nonUniqueValues: {},
    },
    {
      header: 'Покрытие',
      unique: false,
      sampleValues: ['антиналип', 'обычное'],
      hasEveryRow: false,
      nonUniqueValues: {},
    },
    { header: 'Описание', unique: true, sampleValues: [], hasEveryRow: false, nonUniqueValues: {} },
    { header: 'Фото', unique: false, sampleValues: [], hasEveryRow: false, nonUniqueValues: {} },
    { header: 'Название', unique: true, sampleValues: [], hasEveryRow: false, nonUniqueValues: {} },
    {
      header: 'Торговая марка',
      unique: false,
      sampleValues: ['KING', 'Satake', 'Suncraft'],
      hasEveryRow: false,
      nonUniqueValues: {},
    },
    {
      header: 'Материал',
      unique: false,
      sampleValues: ['Керамика', 'дамасская', 'дамасская сталь'],
      hasEveryRow: false,
      nonUniqueValues: {},
    },
    {
      header: 'SKU',
      unique: true,
      sampleValues: ['115416412', '115416415', '115416417'],
      hasEveryRow: false,
      nonUniqueValues: {},
    },
  ],
  processingConfig: {
    barcode: '',
    description: 'описание',
    name: 'название',
    partialLoading: true,
    shopCategory: 'категория',
    shopSku: '',
    vendor: 'торговая марка',
    vendorCode: '',
  },
};

export const importResult = {
  duplicateSkus: [],
  errors: [],
  modelsCount: 0,
  modelsSamples: [],
  processingConfigInfo,
  skusNotFoundCount: 1,
  skusNotFoundSamples: ['578396999'],
  totalRows: 19,
};
