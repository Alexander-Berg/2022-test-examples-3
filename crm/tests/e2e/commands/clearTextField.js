//очистка текстового поля

module.exports = async function(input) {
  await this.execute((s) => {
    s.value = null;
  }, input);
};
