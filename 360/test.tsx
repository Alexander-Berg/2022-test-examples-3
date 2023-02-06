import React, { useCallback, useMemo, useState } from 'react';
import { NextPage } from 'next';
import { cnTheme } from '@yandex-lego/components/Theme';

import mgTheme from '@/components/_common/MGTheme';
import Menu from '@/components/_common/MGMenu';
import Select from '@/components/_common/MGSelect';

const TestPage: NextPage = () => {
  const [value, setValue] = useState<string[]>([]);
  const [searchText, setSearchText] = useState('');
  const onChange = useCallback(value => {
    setValue(value);
  }, []);
  const onChangeSearchText = useCallback(value => {
    setSearchText(value);
  }, []);
  const options = useMemo<string[]>(() => {
    const list = [
      'Каждый',
      'Охотник',
      'Желает',
      'Знать',
      'Где',
      'Сидит',
      'Фазан'
    ];

    return new Array(100).fill(0).reduce((acc, _, index) => {
      return acc.concat(
        Select.createDivider(`Заголовок ${index}`),
        list.map(n => `${n} ${index}`)
      );
    }, []);
  }, []);
  const filteredOptions = useMemo(() => {
    const lowerSearchText = searchText.toLowerCase();

    return options.filter(
      option =>
        Select.isDivider(option) ||
        option.toLowerCase().includes(lowerSearchText)
    );
  }, [options, searchText]);

  return (
    <div className={cnTheme(mgTheme)} style={{ padding: 32 }}>
      <Select
        // autocomplete
        value={value}
        options={filteredOptions}
        searchText={searchText}
        placeholder="Мой магазин"
        onChange={onChange}
        onSearchTextChange={onChangeSearchText}
        buttonProps={{
          theme: 'quarternary',
          size: '40'
        }}
        inputProps={{
          theme: 'outlined',
          size: '36'
        }}
        menuProps={{
          height: 200,
          theme: 'base',
          size: '36',
          marks: 'check'
        }}
      />
      <Menu
        theme="base"
        size="44"
        marks="check"
        value={value}
        items={options}
        onChange={onChange}
        height={200}
        focused
      />
    </div>
  );
};

TestPage.getInitialProps = () => {
  return {};
};

export default TestPage;
