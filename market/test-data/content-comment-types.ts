import { ContentCommentType, Scope } from '@yandex-market/market-proto-dts/Market/Mboc/ContentCommentTypes';

export const contentCommentTypes: ContentCommentType[] = [
  {
    type: 'NO_KNOWLEDGE',
    description: 'Нет знаний в категории',
    require_items: false,
    scope: [Scope.MATCHING],
  },
  {
    type: 'DEPARTMENT_FROZEN',
    description: 'Работы в департаменте заморожены',
    require_items: false,
    scope: [Scope.MATCHING],
  },
  {
    type: 'LEGAL_CONFLICT',
    description: 'Проблемы с правами',
    require_items: false,
    scope: [Scope.MATCHING, Scope.MODERATION],
  },
  {
    type: 'NEED_INFORMATION',
    description: 'Предоставьте информацию о товаре (характеристики модели)',
    require_items: false,
    scope: [Scope.MATCHING],
  },
  {
    type: 'NEED_PICTURES',
    description: 'Предоставьте изображения',
    require_items: false,
    scope: [Scope.MATCHING, Scope.MODERATION],
  },
  {
    type: 'NEED_VENDOR',
    description: 'Укажите вендора',
    require_items: false,
    scope: [Scope.MATCHING, Scope.MODERATION],
  },
  {
    type: 'NO_SIZE_MEASURE',
    description: 'Отсутствует размерная сетка',
    allow_other: 'Другое',
    require_items: true,
    scope: [Scope.MATCHING],
  },
  {
    type: 'NO_PARAMETERS_IN_SHOP_TITLE',
    description: 'В Shop Title не хватает параметров',
    allow_other: 'Другое',
    variant: [
      {
        name: 'Цвет',
      },
      {
        name: 'Размер',
      },
      {
        name: 'Объём',
      },
      {
        name: 'Вес',
      },
      {
        name: 'Количество штук',
      },
      {
        name: 'Вид упаковки',
      },
      {
        name: 'Артикул',
      },
      {
        name: 'Вкус',
      },
      {
        name: 'Габариты',
      },
    ],
    require_items: true,
    scope: [Scope.MATCHING, Scope.MODERATION],
  },
  {
    type: 'CONFLICTING_INFORMATION',
    description: 'Расхождение информации в полях',
    allow_other: 'Другое',
    variant: [
      {
        name: 'Баркод',
      },
      {
        name: 'Артикул',
      },
      {
        name: 'Ссылка на сайт',
      },
      {
        name: 'Ссылка на изображение',
      },
      {
        name: 'Название (Shop title)',
      },
      {
        name: 'Вендор',
      },
      {
        name: 'Описание не соответствует товару',
      },
    ],
    require_items: true,
    scope: [Scope.MATCHING, Scope.MODERATION],
  },
  {
    type: 'INCORRECT_INFORMATION',
    description: 'Неверная информация',
    allow_other: 'Другое',
    require_items: true,
    scope: [Scope.MATCHING, Scope.MODERATION],
  },
  {
    type: 'ASSORTMENT',
    description: 'Товар в ассортименте',
    require_items: false,
    scope: [Scope.MATCHING, Scope.MODERATION],
  },
  {
    type: 'CANCELLED',
    description: 'Отмена обработки позиции по просьбе поставщика/категорийного менеджера',
    require_items: false,
    scope: [Scope.MATCHING, Scope.MODERATION],
  },
  {
    type: 'WRONG_CATEGORY',
    description: 'Неправильная категория',
    allow_other: 'Укажите причину',
    require_items: true,
    scope: [Scope.MATCHING, Scope.MODERATION],
  },
  {
    type: 'FOR_REVISION',
    description: 'Контенту на доработку',
    allow_other: 'Комментарий',
    require_items: true,
    scope: [Scope.MATCHING],
  },
  {
    type: 'NO_SIZE_MEASURE_VALUE',
    description: 'Отсутствуют размеры в размерной сетке',
    allow_other: 'Комментарий',
    require_items: true,
    scope: [Scope.MATCHING],
  },
];
