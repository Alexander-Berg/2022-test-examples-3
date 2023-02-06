import React from 'react';
import { render, screen } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { withSaveValues } from './withSaveValues';

const TestPureComponent = <T extends {} = {}>(props: {
  values: T;
  initialValues: T;
  onSubmit: (values: T) => void;
}) => {
  const handleClick = () => {
    props.onSubmit(props.values);
  };

  return (
    <>
      <p>{JSON.stringify(props.initialValues)}</p>
      <button onClick={handleClick}>submit</button>
    </>
  );
};

describe('withSaveValues', () => {
  it('saves values using onValuesSave', () => {
    let storage: unknown = null;
    const saveValues = jest.fn((values) => {
      storage = values;
    });
    const getSavedValues = () => storage as {} | null;

    const TestComponent = withSaveValues({
      onValuesSave: saveValues,
      getSavedValues,
    })(TestPureComponent);

    const initialValues = {};
    const values = { a: 2, b: 3 };
    const handleSubmit = () => {};

    render(<TestComponent initialValues={initialValues} values={values} onSubmit={handleSubmit} />);

    userEvent.click(screen.getByText('submit'));

    expect(saveValues).toBeCalledTimes(1);
    expect(saveValues).toBeCalledWith(values);
  });

  it('gets initial values using getSavedValues', () => {
    let storage = { a: 1 };
    const saveValues = jest.fn((values) => {
      storage = values;
    });
    const getSavedValues = () => storage as {} | null;

    const TestComponent = withSaveValues({
      onValuesSave: saveValues,
      getSavedValues,
    })(TestPureComponent);

    const initialValues = {};
    const values = {};
    const handleSubmit = () => {};

    render(<TestComponent initialValues={initialValues} values={values} onSubmit={handleSubmit} />);

    expect(screen.getByText(JSON.stringify(storage))).toBeInTheDocument();
  });

  it('filters fields using filter', () => {
    let storage = { a: 1, b: 2, c: 3 };
    const saveValues = jest.fn((values) => {
      storage = values;
    });
    const getSavedValues = () => storage as {} | null;
    const filterCAndA = (key: string) => key !== 'a' && key !== 'c';

    const TestComponent = withSaveValues({
      onValuesSave: saveValues,
      getSavedValues,
    })(TestPureComponent);

    const initialValues = {};
    const values = { d: 1, a: 4, c: 5 };
    const handleSubmit = () => {};

    render(
      <TestComponent
        initialValues={initialValues}
        values={values}
        onSubmit={handleSubmit}
        filter={filterCAndA}
      />,
    );

    expect(screen.getByText(JSON.stringify({ b: 2 }))).toBeInTheDocument();

    userEvent.click(screen.getByText('submit'));

    expect(saveValues).toBeCalledWith({ d: 1 });
  });
});
