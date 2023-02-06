module.exports = async function passportLogin(login, password, retpath) {
    //ускоренный логин (тикет https://st.yandex-team.ru/MFRONT-2956)
    await this.execute(
        (login, password, retpath) => {
            document.querySelectorAll('body')[0].innerHTML = `
      <form action="https://passport.yandex-team.ru/auth?retpath=${retpath}" method="POST">
          <input type="text" name="login" value="${login}">
          <input type="text" name="passwd" value="${password}">
          <input type="submit" value="Auth!" id="fastPassportAuthorization">
      </form>`;
        },
        login,
        password,
        retpath,
    );
    this.$('#fastPassportAuthorization').click();
};
