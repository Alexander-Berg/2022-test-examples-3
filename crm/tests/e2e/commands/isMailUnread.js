module.exports = async function(mail) {
  const url = await this.getUrl();
  if (!url.includes('#/mail')) {
    throw new Error('createDraftMail was called outside mail module test');
  }

  const fontWeight = String((await mail.getCSSProperty('font-weight')).value);
  return fontWeight === '700';
};
