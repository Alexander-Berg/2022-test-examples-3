import { NodeData, TreeNode } from '../TreeView.types';

export interface TestNodeData extends NodeData {
  isArchived?: boolean;
}

export const testData: TreeNode<TestNodeData>[] = [
  {
    id: '1',
    data: { label: 'Авто: товары', isArchived: true },
    items: [
      {
        id: '2',
        data: { label: 'Автотранспорт', isArchived: true },
        items: [
          { id: '20', data: { label: 'Производитель' } },
          { id: '21', data: { label: 'Дилер', isArchived: true } },
          { id: '22', data: { label: 'Агрегаторы' } },
          { id: '23', data: { label: 'Нет специфики', isArchived: true } },
        ],
      },
      {
        id: '3',
        data: { label: 'Коммерческий автотранспорт' },
        items: [
          {
            id: '24',
            data: {
              label: 'Производитель',
              isArchived: true,
            },
          },
          { id: '25', data: { label: 'Дилер', isArchived: true } },
          { id: '26', data: { label: 'Агрегаторы' } },
          { id: '27', data: { label: 'Нет специфики' } },
        ],
      },
    ],
  },
  {
    id: '4',
    data: { label: 'Общая информация' },
    items: [
      { id: '5', data: { label: 'Домен' } },
      { id: '28', data: { label: 'Офисы обслуживания' } },
      { id: '29', data: { label: 'Менеджеры' } },
      { id: '30', data: { label: 'Названия' } },
      { id: '31', data: { label: 'VIP' } },
      {
        id: '6',
        data: { label: 'Контакты' },
        items: [
          { id: '10', data: { label: 'Домен' } },
          {
            id: '11',
            data: { label: 'Названия' },
            items: [{ id: '12', data: { label: 'Менеджеры' } }],
          },
        ],
      },
    ],
  },
  {
    id: '7',
    data: { label: 'Очень-очень-очень длинное длинное название название.' },
    items: [
      { id: '8', data: { label: 'Здоровый сон' } },
      { id: '9', data: { label: 'Прогулки на свежем воздухе' } },
    ],
  },
];
