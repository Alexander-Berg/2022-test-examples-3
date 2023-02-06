import 'intersection-observer';
import flushPromises from 'flush-promises';

import { setupTestApp } from './test/setupApp';
import { Routes } from 'src/Routes';
import { reactRouterToArray } from 'src/utils/reactRouterToArray';
import { INGNORE_PAGE, ROUTER_PATH_CONSTANT } from './AppInt.test.constant';
import { SuppliersPage } from 'src/pages';

function preparePath(path: string): string {
  const regExp = /:(\w+)/g;
  const match = regExp.exec(path)?.[1];

  if (!match || ROUTER_PATH_CONSTANT[match] === undefined) return path;

  return path.replace(/(:.*\/)|(:.*)/g, ROUTER_PATH_CONSTANT[match]);
}

describe('<AppInit/>', () => {
  afterEach(async () => {
    try {
      await flushPromises();
    } catch (e) {
      console.log(e);
    }
  });

  it('renders routes correctly', () => {
    const { app } = setupTestApp('/');
    expect(app.find(SuppliersPage)).toHaveLength(1);
    // document.location.href = '#/settings'

    // app.update();
    // console.log(JSON.stringify(SettingsPage))
    // const wrapper = shallow(<SettingsPage />);
    //
    // expect(app.matchesElement(SettingsPage)).toEqual(true);
    // expect(app.text()).toContain(wrapper.text());
    //  expect(app.contains(component)).toEqual(true);
    // expect(app.find(SuppliersPage)).toHaveLength(1);

    // const documentsMenuLink = app.find('[href="#/documents"]').find('a');
    // app.find(SuppliersPage).prop('location').pathname = '/documents';
    // (documentsMenuLink.prop('onClick'))();
    // documentsMenuLink.simulate('click', { button: 0 });
    // document.location.href = '#/documents';
    // app.update();
    // expect(app.find(DocumentPage).props().location.pathname).toBe("/documents");
    // console.log(app.debug());
    // expect(app.find(SuppliersPage).debug()).toHaveLength(0);
    // expect(app.find(DocumentPage)).toHaveLength(1);
    app.unmount();
  });

  describe('Check working routes', () => {
    const setLayoutTheme = () => null;
    const { app, history, api } = setupTestApp('/');
    const newTest = Routes({ setLayoutTheme, api });
    const arrayRoutes = reactRouterToArray(newTest);

    for (const item of arrayRoutes) {
      if (item && !INGNORE_PAGE[item.path]) {
        const path = preparePath(item.path);

        it(item.path, () => {
          history.replace(path);
          app.update();

          const component = item.component;

          if (!component) return;

          expect(app.find(component)).toHaveLength(1);
        });
      }
    }
  });
});
