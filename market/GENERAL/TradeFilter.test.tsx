import React from 'react';
import { render, screen, fireEvent } from '@testing-library/react';
import { createStore } from 'redux';

import * as selectors from 'src/pages/promo/store/selectors';
import { TradeFilterProps as ComponentProps, TradeFilter, Option, FILTER_NAME } from './TradeFilter';
import { rootReducer } from 'src/store/root/reducer';
import { WrapperWithStore } from 'src/pages/promo/tests/WrapperWithStore';

const ALLOWED_LOGINS_MOCK: string[] = ['login1', 'login2', 'login3'];
const store = createStore(rootReducer);

function createLoginOption(login: string) {
  return {
    label: login,
    value: login,
  };
}

describe('<TradeFilter />', () => {
  const DEFAULT_PROPS: ComponentProps = {
    onChange: jest.fn(),
    selectedValues: null,
  };

  beforeEach(() => {
    jest.spyOn(selectors, 'selectAllowedTradesStaffLogins').mockImplementation(() => ALLOWED_LOGINS_MOCK);
  });

  afterEach(() => {
    jest.clearAllMocks();
  });

  const renderWithProps = (props: ComponentProps = DEFAULT_PROPS) => {
    return render(
      <WrapperWithStore store={store}>
        <TradeFilter {...props} />
      </WrapperWithStore>
    );
  };

  const getInput = () => screen.getByRole('textbox');
  const selectOption = (option: Option) => {
    const input = getInput();
    fireEvent.change(input, { target: { value: option.label } });
    fireEvent.keyDown(input, { key: 'Enter', code: 'Enter' });
  };

  it('render without errors and contains input with empty values', () => {
    renderWithProps();

    const input = getInput();
    expect(input).toBeInTheDocument();
    expect(input).toHaveProperty('value', '');
  });

  it('changing of input calls the callback', async () => {
    renderWithProps();
    const loginForSelect = ALLOWED_LOGINS_MOCK[0];

    const optionForSelect = createLoginOption(loginForSelect);
    selectOption(optionForSelect);

    expect(DEFAULT_PROPS.onChange).toHaveBeenLastCalledWith({ [FILTER_NAME]: [optionForSelect.value] });
    expect(getInput()).toHaveProperty('value', '');
  });

  it('changing of non-empty input calls the callback', async () => {
    const selectedLogin = ALLOWED_LOGINS_MOCK[0];
    const selectedOption = createLoginOption(selectedLogin);

    const loginForSelect = ALLOWED_LOGINS_MOCK[1];
    const optionForSelect = createLoginOption(loginForSelect);

    renderWithProps({
      ...DEFAULT_PROPS,
      selectedValues: [selectedOption.value],
    });

    selectOption(optionForSelect);

    expect(DEFAULT_PROPS.onChange).toHaveBeenLastCalledWith({
      [FILTER_NAME]: [selectedOption.value, optionForSelect.value],
    });
    expect(getInput()).toHaveProperty('value', '');
  });
});
