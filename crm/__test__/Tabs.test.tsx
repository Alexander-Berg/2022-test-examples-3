import React from 'react';
import { render, screen, fireEvent } from '@testing-library/react';
import { Tabs } from '../Tabs';
import { Tab } from '../Tab';

describe('Tabs', () => {
  it('should support headerLeft prop', () => {
    render(
      <Tabs headerLeft="headerLeft">
        <Tab title="Tab 1">Tab 1</Tab>
        <Tab title="Tab 2">Tab 2</Tab>
      </Tabs>,
    );

    screen.getByText('headerLeft');
  });

  it('should support title prop', () => {
    render(
      <Tabs title="title">
        <Tab title="Tab 1">Tab 1</Tab>
        <Tab title="Tab 2">Tab 2</Tab>
      </Tabs>,
    );

    screen.getByText('title');
  });

  it('should support content prop', () => {
    render(
      <Tabs content="content">
        <Tab title="Tab 1">Tab 1</Tab>
        <Tab title="Tab 2">Tab 2</Tab>
      </Tabs>,
    );

    screen.getByText('content');
  });

  it('should render two tab buttons', () => {
    render(
      <Tabs title="title">
        <Tab title="Tab 1">Content 1</Tab>
        <Tab title="Tab 2">Content 2</Tab>
      </Tabs>,
    );

    const buttons = screen.getAllByRole('button');

    expect(buttons).toHaveLength(2);
    expect(buttons[0]).toHaveTextContent('Tab 1');
    expect(buttons[1]).toHaveTextContent('Tab 2');
  });

  it('should support onDidUpdate props', () => {
    const onDidUpdate = jest.fn();

    render(
      <Tabs title="title" onDidUpdate={onDidUpdate}>
        <Tab title="Tab 1">Content 1</Tab>
        <Tab title="Tab 2">Content 2</Tab>
      </Tabs>,
    );
    fireEvent.click(screen.getByText('Tab 2'));

    expect(onDidUpdate).toBeCalledTimes(1);
    expect(onDidUpdate).toBeCalledWith(0, 1);
  });

  describe('uncontrolled', () => {
    it('should display first tab by default', () => {
      render(
        <Tabs title="title">
          <Tab title="Tab 1">Content 1</Tab>
          <Tab title="Tab 2">Content 2</Tab>
        </Tabs>,
      );

      screen.getByText('Content 1');
    });

    it('should support defaultValue prop', () => {
      render(
        <Tabs defaultValue={1}>
          <Tab title="Tab 1">Content 1</Tab>
          <Tab title="Tab 2">Content 2</Tab>
        </Tabs>,
      );

      screen.getByText('Content 2');
    });

    it('should support switch tab', () => {
      render(
        <Tabs title="title">
          <Tab title="Tab 1">Content 1</Tab>
          <Tab title="Tab 2">Content 2</Tab>
        </Tabs>,
      );
      fireEvent.click(screen.getByText('Tab 2'));
      screen.getByText('Content 2');
    });

    it('should support canHide prop', () => {
      render(
        <Tabs canHide>
          <Tab title="Tab 1">Content 1</Tab>
          <Tab title="Tab 2">Content 2</Tab>
        </Tabs>,
      );
      fireEvent.click(screen.getByText('Tab 1'));
      expect(screen.queryByText(/content/i)).toBeNull();
    });
  });

  describe('controlled', () => {
    const emptyOnChange = () => {};
    it('should use child index as tab id', () => {
      render(
        <Tabs value={1} onChange={emptyOnChange}>
          <Tab title="Tab 1">Content 1</Tab>
          <Tab title="Tab 2">Content 2</Tab>
        </Tabs>,
      );

      screen.getByText('Content 2');
    });

    it('should use value as tab id', () => {
      render(
        <Tabs value="tab2" onChange={emptyOnChange}>
          <Tab title="Tab 1" value="tab1">
            Content 1
          </Tab>
          <Tab title="Tab 2" value="tab2">
            Content 2
          </Tab>
        </Tabs>,
      );

      screen.getByText('Content 2');
    });

    it('should support canHide prop', () => {
      const onChange = jest.fn();

      render(
        <Tabs canHide value="tab1" onChange={onChange}>
          <Tab title="Tab 1" value="tab1">
            Content 1
          </Tab>
          <Tab title="Tab 2" value="tab1">
            Content 2
          </Tab>
        </Tabs>,
      );
      fireEvent.click(screen.getByText('Tab 1'));
      expect(onChange).toBeCalledTimes(1);
      expect(onChange).toBeCalledWith(-1);
    });

    it('should no content', () => {
      render(
        <Tabs onChange={emptyOnChange}>
          <Tab title="Tab 1">Content 1</Tab>
          <Tab title="Tab 2">Content 2</Tab>
        </Tabs>,
      );

      expect(screen.queryByText(/content/i)).toBeNull();
    });
  });
});
