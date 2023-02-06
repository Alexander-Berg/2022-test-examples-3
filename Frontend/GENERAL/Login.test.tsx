import React from 'react';
import { getJson } from '../internal/getJson';
import { Login } from '.';

describe('Login', () => {
  test('Правильно прокидывает свойства в Link', () => {
    expect(getJson(<Login target="_blank" />).props.target).toEqual('_blank');
  });
});
