import React from 'react';
import { render } from '@testing-library/react';

import { BlueWoodpeckerHouse } from './BlueWoodpeckerHouse';
import { TestingRouter } from 'src/test/setupApp';

const peckers = [
  {
    name: 'ParameterTutorialVideo',
    querySelector: '.ParameterTutorialVideo',
    content: 'ParameterTutorialVideo',
    expires: new Date(2040, 2, 22),
  },
  {
    name: 'CategoryTutorialVideo',
    querySelector: '.CategoryTutorialVideo',
    content: 'CategoryTutorialVideo',
    expires: new Date(2020, 2, 22),
  },
  {
    name: 'ChangeValue',
    querySelector: '.ChangeValue',
    content: 'ChangeValue',
    expires: new Date(2040, 2, 22),
  },
];

describe('BlueWoodpeckerHouse', () => {
  test('show peckers', async () => {
    window.localStorage.setItem('ChangeValue', 'true');
    const app = render(
      <TestingRouter route="/parameter">
        <div>
          {peckers.map(el => (
            <div className={el.name} />
          ))}
          <BlueWoodpeckerHouse peckers={peckers} />
        </div>
      </TestingRouter>
    );

    await app.findByText(peckers[0].content);
    // вторая подсказка не должна выводится так как истекла дата актуальности
    expect(app.queryByText(peckers[1].content)).toBeFalsy();
    // 3-я не должна выводится так как уже была активирована
    expect(app.queryByText(peckers[2].content)).toBeFalsy();
  });

  test('checked peckers', async () => {
    window.localStorage.setItem('ChangeValue', 'true');
    const app = render(
      <TestingRouter route="/parameter">
        <div>
          {peckers.map(el => (
            <div className={el.name} />
          ))}
          <BlueWoodpeckerHouse peckers={peckers} />
        </div>
      </TestingRouter>
    );

    expect(app.queryByText(peckers[2].content)).toBeFalsy();
  });
});
