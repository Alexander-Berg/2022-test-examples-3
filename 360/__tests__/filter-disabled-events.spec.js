import EventRecord from 'features/events/EventRecord';
import LayerRecord from 'features/layers/LayerRecord';

import filterDisabledEvents from '../filter-disabled-events';

describe('filter-disabled-events', () => {
  it('должен отфильтровывать события выключенных слоёв', () => {
    const layers = [
      new LayerRecord({id: 1, isToggledOn: true}),
      new LayerRecord({id: 2, isToggledOn: false}),
      new LayerRecord({id: 3, isToggledOn: true})
    ];

    const events = [
      new EventRecord({layerId: 1}), // A
      new EventRecord({layerId: 2}), // B
      new EventRecord({layerId: 3}) //  C
    ];

    const result = filterDisabledEvents(layers, events);

    expect(result).toHaveLength(2);
    expect(result).toMatchObject([
      events[0], // A
      events[2] //  C
    ]);
  });

  it('должен отфильтровывать события неизвестных слоёв', () => {
    const layers = [
      new LayerRecord({id: 1, isToggledOn: true}),
      new LayerRecord({id: 2, isToggledOn: false})
    ];

    const events = [
      new EventRecord({layerId: 1}), // A
      new EventRecord({layerId: 2}), // B
      new EventRecord({layerId: 3}) //  C. неизвестный слой
    ];

    const result = filterDisabledEvents(layers, events);

    expect(result).toHaveLength(1);
    expect(result).toMatchObject([
      events[0] // A
    ]);
  });

  it('не должен отфильтровывать события, когда массив слоёв пуст', () => {
    const layers = [];

    const events = [
      new EventRecord({layerId: 1}),
      new EventRecord({layerId: 2}),
      new EventRecord({layerId: 3})
    ];

    const result = filterDisabledEvents(layers, events);

    expect(result).toHaveLength(3);
  });
});
