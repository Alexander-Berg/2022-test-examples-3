import { ResetForcedDispatch } from './ResetForcedDispatch';
import { setupTestApp } from 'src/test/setupApp';
import { User } from 'src/java/definitions';
import { resolveConfig } from 'src/test/commonResolve';

describe('ResetForcedDispatch', () => {
  it('should be render without errors', () => {
    const { app, api } = setupTestApp(`/suppliers/0`);
    api.currentUserController.getCurrentUser.next().resolve({ roles: ['INTERNAL_PROCESS'] } as User);
    resolveConfig(api);

    app
      .findWhere(node => {
        return node.type() === 'button' && node.text() === 'Изменить';
      })
      .simulate('click');

    expect(app.find(ResetForcedDispatch)).toHaveLength(1);

    app.unmount();
  });
});
