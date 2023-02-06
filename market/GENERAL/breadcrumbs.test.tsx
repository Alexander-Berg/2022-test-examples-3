import Enzyme, { shallow } from "enzyme";
import Adapter from "enzyme-adapter-react-16";
import expect from "expect.js";
import * as React from "react";
import { Link } from "redux-little-router";
import { RELEASE_DETAILS_PAGE_ABSOLUTE } from "routes";
/* eslint-env node, mocha */
import { Breadcrumb, Breadcrumbs } from "./breadcrumbs";

Enzyme.configure({ adapter: new Adapter() });

describe("<Breadcrumbs />", () => {
  const releaseGroup = {
    id: "tsum-stage-group",
    stageGroupId: "tsum-stage-group",
    title: "Пайплайны ЦУМа",
  };
  const project = {
    id: "test-project",
    title: "Тестовый проект",
    deliveryMachines: [releaseGroup],
  };
  const releaseTitle = "Релиз номер 1 в России";
  const activeBreadcrumbTitle = "Активная хлебная крошка";

  it("should render without stage group", () => {
    const wrapper = shallow(
      <Breadcrumbs project={project}>
        <Breadcrumb
          route={RELEASE_DETAILS_PAGE_ABSOLUTE}
          params={{ releaseId: "release-1" }}
        >
          {releaseTitle}
        </Breadcrumb>
        <Breadcrumb active>{activeBreadcrumbTitle}</Breadcrumb>
      </Breadcrumbs>,
    );

    const ol = wrapper.find("ol");
    expect(ol.children()).to.have.length(4);
    let i = 0;
    expect(
      ol
        .childAt(i++)
        .find(Link)
        .childAt(0)
        .text(),
    ).to.eql("Проекты");
    expect(
      ol
        .childAt(i++)
        .find(Link)
        .childAt(0)
        .text(),
    ).to.eql(project.title);
    expect(
      ol
        .childAt(i++)
        .find(Link)
        .childAt(0)
        .text(),
    ).to.eql(releaseTitle);
    expect(
      ol
        .childAt(i++)
        .childAt(0)
        .text(),
    ).to.eql(activeBreadcrumbTitle);
  });

  it("should render with stage group", () => {
    const wrapper = shallow(
      <Breadcrumbs project={project} releaseGroup={releaseGroup}>
        <Breadcrumb
          route={RELEASE_DETAILS_PAGE_ABSOLUTE}
          params={{ releaseId: "release-1" }}
        >
          {releaseTitle}
        </Breadcrumb>
        <Breadcrumb active>{activeBreadcrumbTitle}</Breadcrumb>
      </Breadcrumbs>,
    );

    const ol = wrapper.find("ol");
    expect(ol.children()).to.have.length(6);
    let i = 0;
    expect(
      ol
        .childAt(i++)
        .find(Link)
        .childAt(0)
        .text(),
    ).to.eql("Проекты");
    expect(
      ol
        .childAt(i++)
        .find(Link)
        .childAt(0)
        .text(),
    ).to.eql(project.title);
    expect(
      ol
        .childAt(i++)
        .find(Link)
        .childAt(0)
        .text(),
    ).to.eql("Релизные машины");
    expect(
      ol
        .childAt(i++)
        .find(Link)
        .childAt(0)
        .text(),
    ).to.eql(releaseGroup.title);
    expect(
      ol
        .childAt(i++)
        .find(Link)
        .childAt(0)
        .text(),
    ).to.eql(releaseTitle);
    expect(
      ol
        .childAt(i++)
        .childAt(0)
        .text(),
    ).to.eql(activeBreadcrumbTitle);
  });
});
