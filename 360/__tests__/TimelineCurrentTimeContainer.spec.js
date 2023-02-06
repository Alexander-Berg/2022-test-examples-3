import React from 'react';
import moment from 'moment';
import {shallow} from 'enzyme';

import {TimelineCurrentTimeContainer} from '../TimelineCurrentTimeContainer';

describe('<TimelineCurrentTimeContainer />', () => {
  test('должен отрисовать полоску для 15:01:00', () => {
    const component = shallow(
      <TimelineCurrentTimeContainer
        currentTime={moment('2017-09-21T15:01:00').valueOf()}
        timelineDate={moment('2017-09-21').valueOf()}
      />
    );

    expect(component).toMatchSnapshot();
  });

  test('должен отрисовать полоску для 08:00:00', () => {
    const component = shallow(
      <TimelineCurrentTimeContainer
        currentTime={moment('2017-09-21T08:00:00').valueOf()}
        timelineDate={moment('2017-09-21').valueOf()}
      />
    );

    expect(component).toMatchSnapshot();
  });

  test('должен отрисовать полоску для 23:00:00', () => {
    const component = shallow(
      <TimelineCurrentTimeContainer
        currentTime={moment('2017-09-21T23:00:00').valueOf()}
        timelineDate={moment('2017-09-21').valueOf()}
      />
    );

    expect(component).toMatchSnapshot();
  });

  test('не должен отрисовать полоску для 00:00:01', () => {
    const component = shallow(
      <TimelineCurrentTimeContainer
        currentTime={moment('2017-09-22T00:00:01').valueOf()}
        timelineDate={moment('2017-09-21').valueOf()}
      />
    );

    expect(component).toMatchSnapshot();
  });

  test('не должен отрисовать полоску для 07:59:59', () => {
    const component = shallow(
      <TimelineCurrentTimeContainer
        currentTime={moment('2017-09-21T07:59:59').valueOf()}
        timelineDate={moment('2017-09-21').valueOf()}
      />
    );

    expect(component).toMatchSnapshot();
  });

  test('не должен отрисовать полоску для 00:00:00', () => {
    const component = shallow(
      <TimelineCurrentTimeContainer
        currentTime={moment('2017-09-21T00:00:00').valueOf()}
        timelineDate={moment('2017-09-21').valueOf()}
      />
    );

    expect(component).toMatchSnapshot();
  });
});
