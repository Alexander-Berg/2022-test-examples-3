import { RegionDTO } from 'src/java/definitions-replenishment';
import { regionsSort } from './mappers';

const regions: RegionDTO[] = [
  {
    id: 3,
    parentId: 225,
    type: 4,
    name: 'Центральный федеральный округ',
  },
  {
    id: 17,
    parentId: 225,
    type: 4,
    name: 'Северо-Западный федеральный округ',
  },
  {
    id: 26,
    parentId: 225,
    type: 4,
    name: 'Южный федеральный округ',
  },
  {
    id: 40,
    parentId: 225,
    type: 4,
    name: 'Приволжский федеральный округ',
  },
  {
    id: 52,
    parentId: 225,
    type: 4,
    name: 'Уральский федеральный округ',
  },
  {
    id: 59,
    parentId: 225,
    type: 4,
    name: 'Сибирский федеральный округ',
  },
  {
    id: 73,
    parentId: 225,
    type: 4,
    name: 'Дальневосточный федеральный округ',
  },
  {
    id: 102444,
    parentId: 225,
    type: 4,
    name: 'Северо-Кавказский федеральный округ',
  },
  {
    id: 10174,
    parentId: 17,
    type: 5,
    name: 'Санкт-Петербург и Ленинградская область',
  },
  {
    id: 10176,
    parentId: 17,
    type: 5,
    name: 'Ненецкий автономный округ',
  },
  {
    id: 10842,
    parentId: 17,
    type: 5,
    name: 'Архангельская область',
  },
  {
    id: 10853,
    parentId: 17,
    type: 5,
    name: 'Вологодская область',
  },
  {
    id: 10857,
    parentId: 17,
    type: 5,
    name: 'Калининградская область',
  },
  {
    id: 10897,
    parentId: 17,
    type: 5,
    name: 'Мурманская область',
  },
  {
    id: 10904,
    parentId: 17,
    type: 5,
    name: 'Новгородская область',
  },
  {
    id: 10926,
    parentId: 17,
    type: 5,
    name: 'Псковская область',
  },
  {
    id: 10933,
    parentId: 17,
    type: 5,
    name: 'Республика Карелия',
  },
  {
    id: 10939,
    parentId: 17,
    type: 5,
    name: 'Республика Коми',
  },
  {
    id: 2,
    parentId: 10174,
    type: 6,
    name: 'Санкт-Петербург',
  },
];
const sortRegions: RegionDTO[] = [
  { id: 3, name: 'Центральный федеральный округ', parentId: 225, type: 4 },
  { id: 17, name: 'Северо-Западный федеральный округ', parentId: 225, type: 4 },
  { id: 10174, name: 'Санкт-Петербург и Ленинградская область', parentId: 17, type: 5 },
  { id: 2, name: 'Санкт-Петербург', parentId: 10174, type: 6 },
  { id: 10176, name: 'Ненецкий автономный округ', parentId: 17, type: 5 },
  { id: 10842, name: 'Архангельская область', parentId: 17, type: 5 },
  { id: 10853, name: 'Вологодская область', parentId: 17, type: 5 },
  { id: 10857, name: 'Калининградская область', parentId: 17, type: 5 },
  { id: 10897, name: 'Мурманская область', parentId: 17, type: 5 },
  { id: 10904, name: 'Новгородская область', parentId: 17, type: 5 },
  { id: 10926, name: 'Псковская область', parentId: 17, type: 5 },
  { id: 10933, name: 'Республика Карелия', parentId: 17, type: 5 },
  { id: 10939, name: 'Республика Коми', parentId: 17, type: 5 },
  { id: 26, name: 'Южный федеральный округ', parentId: 225, type: 4 },
  { id: 40, name: 'Приволжский федеральный округ', parentId: 225, type: 4 },
  { id: 52, name: 'Уральский федеральный округ', parentId: 225, type: 4 },
  { id: 59, name: 'Сибирский федеральный округ', parentId: 225, type: 4 },
  { id: 73, name: 'Дальневосточный федеральный округ', parentId: 225, type: 4 },
  { id: 102444, name: 'Северо-Кавказский федеральный округ', parentId: 225, type: 4 },
];
describe('src/pages/replenishment/utils/mappers', () => {
  it('regionsSort', () => {
    expect(regionsSort(regions)).toEqual(sortRegions);
  });
});
