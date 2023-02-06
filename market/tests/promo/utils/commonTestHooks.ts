import { getUserFromEnvOrYav } from '../../../utils/user';

export async function commonBeforeEach() {
  const user = await getUserFromEnvOrYav();

  await this.browser
    .yaLogin(user.login, user.password)
    .url('/');

  const { HERMIONE_FRONTEND_VERSION } = process.env;
  if (HERMIONE_FRONTEND_VERSION) {
    await this.browser.yaSetCookie({
      name: 'static_version',
      value: HERMIONE_FRONTEND_VERSION,
    });
  }

  await this.browser.url('/');
}

export async function commonAfterEach() {
  await this.browser.yaLogout();
}
