/* eslint-env node, mocha */
import Enzyme, { shallow } from "enzyme";
import Adapter from "enzyme-adapter-react-16";
import expect from "expect.js";
import VerticalTabs, { Tab } from "features/common/components/verticalTabs";
import * as React from "react";
import { Link } from "redux-little-router";

Enzyme.configure({ adapter: new Adapter() });

describe("<VerticalTabs />", () => {
  it("should render active tab without link", () => {
    const wrapper = shallow(
      <VerticalTabs>
        <Tab active href="https://ya.ru">
          Яндекс
        </Tab>
      </VerticalTabs>,
    );

    expect(
      wrapper
        .find("ul")
        .find(Tab)
        .dive()
        .childAt(0)
        .is(Link),
    ).to.eql(false);
  });

  it("should render non-active tab with link", () => {
    const wrapper = shallow(
      <VerticalTabs>
        <Tab href="https://ya.ru">Яндекс</Tab>
      </VerticalTabs>,
    );

    expect(
      wrapper
        .find("ul")
        .find(Tab)
        .dive()
        .childAt(0)
        .type(),
    ).to.eql(Link);
  });

  it("should react on click", () => {
    let handlerCalled = false;
    const onClick = () => (handlerCalled = true);

    const wrapper = shallow(
      <VerticalTabs>
        <Tab onClick={onClick}>Яндекс</Tab>
      </VerticalTabs>,
    );

    wrapper
      .find("ul")
      .find(Tab)
      .simulate("click");
    expect(handlerCalled).to.be(true);
  });

  it("should not wrap tab to link if href was not passed", () => {
    const wrapper = shallow(
      <VerticalTabs>
        <Tab>
          <a href="https://ya.ru">Яндекс</a>
        </Tab>
      </VerticalTabs>,
    );

    expect(
      wrapper
        .find("ul")
        .find(Tab)
        .dive()
        .childAt(0)
        .type(),
    ).to.eql("a");
  });
});
