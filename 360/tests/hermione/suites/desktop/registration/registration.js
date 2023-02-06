const { makeLegalDataCreator } = require('../../../faker/registration-legal');

describe('Registration', () => {
  beforeEach(function() {
    return this.browser.yaAuthRandom();
  });

  /**
   * @see https://testpalm2.yandex-team.ru/testcase/pay-32
   */
  hermione.config.testTimeout(120000);
  it('Регистрация ИП', async function() {
    const PO = this.PO;
    const legalData = await this.browser.yaUseFaker(
      makeLegalDataCreator({ withIPType: true })
    );

    return (
      this.browser
        // - do: пройти первый шаг регистрации
        .yaSkipRegistrationInitialStep({ withIPType: true })
        // - do: открыть страницу '/registration/legal'
        .url('/registration/legal')
        // - do: корректно заполнить форму
        .click(PO.registration.legal.organizationIPTypeCheckbox())
        .yaClearValue(PO.registration.legal.ceoPersonSurnameInput())
        .setValue(
          PO.registration.legal.ceoPersonSurnameInput(),
          legalData.persons.ceo.surname
        )
        .yaClearValue(PO.registration.legal.ceoPersonNameInput())
        .setValue(
          PO.registration.legal.ceoPersonNameInput(),
          legalData.persons.ceo.name
        )
        .yaClearValue(PO.registration.legal.ceoPersonPatronymicInput())
        .setValue(
          PO.registration.legal.ceoPersonPatronymicInput(),
          legalData.persons.ceo.patronymic
        )
        .setValue(
          PO.registration.legal.ceoPersonBirthDateInput(),
          legalData.persons.ceo.birthDate
        )
        .setValue(
          PO.registration.legal.organizationEnglishNameInput(),
          legalData.organization.englishName
        )
        .yaClearValue(PO.registration.legal.organizationOGRNInput())
        .setValue(
          PO.registration.legal.organizationOGRNInput(),
          legalData.organization.ogrn
        )
        // .yaClearValue(PO.registration.legal.organizationINNInput())
        // .setValue(
        //   PO.registration.legal.organizationINNInput(),
        //   legalData.organization.inn
        // )
        .setValue(
          PO.registration.legal.organizationScheduleInput(),
          legalData.organization.scheduleText
        )
        .setValue(
          PO.registration.legal.organizationDescriptionInput(),
          legalData.organization.description
        )
        .setValue(
          PO.registration.legal.organizationSiteUrlInput(),
          legalData.organization.siteUrl
        )
        .setValue(
          PO.registration.legal.ceoPersonPhoneInput(),
          legalData.persons.ceo.phone
        )
        .setValue(
          PO.registration.legal.ceoPersonEmailInput(),
          legalData.persons.ceo.email
        )
        .setValue(
          PO.registration.legal.legalAddressZipInput(),
          legalData.addresses.legal.zip
        )
        .setValue(
          PO.registration.legal.legalAddressCityInput(),
          legalData.addresses.legal.city
        )
        .setValue(
          PO.registration.legal.legalAddressStreetInput(),
          legalData.addresses.legal.street
        )
        .setValue(
          PO.registration.legal.legalAddressHomeInput(),
          legalData.addresses.legal.home
        )
        .setValue(PO.registration.legal.bankBikInput(), legalData.bank.bik)
        .yaLoseFocus()
        .waitForEnabled(PO.registration.legal.bankNameInput())
        .setValue(PO.registration.legal.bankNameInput(), legalData.bank.name)
        .setValue(
          PO.registration.legal.bankCorrespondentAccountInput(),
          legalData.bank.correspondentAccount
        )
        .setValue(
          PO.registration.legal.bankAccountInput(),
          legalData.bank.account
        )
        // - do: нажать на "Сохранить и продолжить"
        .click(PO.registration.legal.startLegalDataSavingButton())
        // - assert: открылась страница подписания оферты
        .yaWaitForPage('/registration/documents')
        // - do: загрузить документы
        .yaSetFileInputValue(PO.registration.documents.offerInput())
        .waitForVisible(PO.registration.documents.offerAttachGroup())
        .yaSetFileInputValue(PO.registration.documents.passportInput())
        .waitForVisible(PO.registration.documents.passportAttachGroup())
        // - do: нажать на "Отправить на проверку"
        .click(PO.registration.documents.startModerationButton())
        // - assert: открылась страница модерации
        .yaWaitForPage('/registration/moderation/ongoing')
    );
  });

  /**
   * @see https://testpalm2.yandex-team.ru/testcase/pay-31
   */
  hermione.config.testTimeout(120000);
  it('Регистрация Юр. лица', async function() {
    const PO = this.PO;
    const legalData = await this.browser.yaUseFaker(
      makeLegalDataCreator({ withIPType: false })
    );

    return (
      this.browser
        // - do: пройти первый шаг регистрации
        .yaSkipRegistrationInitialStep({ withIPType: false })
        // - do: открыть страницу '/registration/legal'
        .url('/registration/legal')
        // - do: корректно заполнить форму
        .click(PO.registration.legal.organizationOOOTypeCheckbox())
        .yaClearValue(PO.registration.legal.organizationFullNameInput())
        .setValue(
          PO.registration.legal.organizationFullNameInput(),
          legalData.organization.fullName
        )
        .yaClearValue(PO.registration.legal.organizationNameInput())
        .setValue(
          PO.registration.legal.organizationNameInput(),
          legalData.organization.name
        )
        .yaClearValue(PO.registration.legal.organizationEnglishNameInput())
        .setValue(
          PO.registration.legal.organizationEnglishNameInput(),
          legalData.organization.englishName
        )
        .yaClearValue(PO.registration.legal.organizationOGRNInput())
        .setValue(
          PO.registration.legal.organizationOGRNInput(),
          legalData.organization.ogrn
        )
        // .yaClearValue(PO.registration.legal.organizationINNInput())
        // .setValue(
        //   PO.registration.legal.organizationINNInput(),
        //   legalData.organization.inn
        // )
        .yaClearValue(PO.registration.legal.organizationKPPInput())
        .setValue(
          PO.registration.legal.organizationKPPInput(),
          legalData.organization.kpp
        )
        .setValue(
          PO.registration.legal.organizationScheduleInput(),
          legalData.organization.scheduleText
        )
        .setValue(
          PO.registration.legal.organizationDescriptionInput(),
          legalData.organization.description
        )
        .setValue(
          PO.registration.legal.organizationSiteUrlInput(),
          legalData.organization.siteUrl
        )
        .yaClearValue(PO.registration.legal.legalAddressZipInput())
        .setValue(
          PO.registration.legal.legalAddressZipInput(),
          legalData.addresses.legal.zip
        )
        .yaClearValue(PO.registration.legal.legalAddressCityInput())
        .setValue(
          PO.registration.legal.legalAddressCityInput(),
          legalData.addresses.legal.city
        )
        .yaClearValue(PO.registration.legal.legalAddressStreetInput())
        .setValue(
          PO.registration.legal.legalAddressStreetInput(),
          legalData.addresses.legal.street
        )
        .yaClearValue(PO.registration.legal.legalAddressHomeInput())
        .setValue(
          PO.registration.legal.legalAddressHomeInput(),
          legalData.addresses.legal.home
        )
        .yaClearValue(PO.registration.legal.ceoPersonSurnameInput())
        .setValue(
          PO.registration.legal.ceoPersonSurnameInput(),
          legalData.persons.ceo.surname
        )
        .yaClearValue(PO.registration.legal.ceoPersonNameInput())
        .setValue(
          PO.registration.legal.ceoPersonNameInput(),
          legalData.persons.ceo.name
        )
        .yaClearValue(PO.registration.legal.ceoPersonPatronymicInput())
        .setValue(
          PO.registration.legal.ceoPersonPatronymicInput(),
          legalData.persons.ceo.patronymic
        )
        .setValue(
          PO.registration.legal.ceoPersonBirthDateInput(),
          legalData.persons.ceo.birthDate
        )
        .yaClearValue(PO.registration.legal.ceoPersonPhoneInput())
        .setValue(
          PO.registration.legal.ceoPersonPhoneInput(),
          legalData.persons.ceo.phone
        )
        .setValue(
          PO.registration.legal.ceoPersonEmailInput(),
          legalData.persons.ceo.email
        )
        .setValue(PO.registration.legal.bankBikInput(), legalData.bank.bik)
        .yaLoseFocus()
        .waitForEnabled(PO.registration.legal.bankNameInput())
        .setValue(PO.registration.legal.bankNameInput(), legalData.bank.name)
        .setValue(
          PO.registration.legal.bankCorrespondentAccountInput(),
          legalData.bank.correspondentAccount
        )
        .setValue(
          PO.registration.legal.bankAccountInput(),
          legalData.bank.account
        )
        // - do: нажать на "Сохранить и продолжить"
        .click(PO.registration.legal.startLegalDataSavingButton())
        // - assert: открылась страница подписания оферты
        .yaWaitForPage('/registration/documents')
        // - do: загрузить документы
        .yaSetFileInputValue(PO.registration.documents.offerInput())
        .waitForVisible(PO.registration.documents.offerAttachGroup())
        .yaSetFileInputValue(PO.registration.documents.passportInput())
        .waitForVisible(PO.registration.documents.passportAttachGroup())
        // - do: нажать на "Отправить на проверку"
        .click(PO.registration.documents.startModerationButton())
        // - assert: открылась страница модерации
        .yaWaitForPage('/registration/moderation/ongoing')
    );
  });

  /**
   * @see https://testpalm2.yandex-team.ru/testcase/pay-40
   */
  it('Загрузка документов (подписант != рук-ль)', function() {
    const PO = this.PO;

    return (
      this.browser
        // - do: пройти первый и второй шаги регистрации
        .yaSkipRegistrationInitialStep({ withIPType: true })
        .yaSkipRegistrationLegalStep({ withIPType: true })
        // - do: открыть страницу '/registration/documents'
        .url('/registration/documents')
        // - do: нажать на "Руководитель является подписантом"
        .click(PO.registration.documents.withSignerDocumentsCheckbox())
        // - do: загрузить документы
        .yaSetFileInputValue(PO.registration.documents.offerInput())
        .waitForVisible(PO.registration.documents.offerAttachGroup())
        .yaSetFileInputValue(PO.registration.documents.passportInput())
        .waitForVisible(PO.registration.documents.passportAttachGroup())
        .yaSetFileInputValue(PO.registration.documents.signerPassportInput())
        .waitForVisible(PO.registration.documents.signerPassportAttachGroup())
        .yaSetFileInputValue(PO.registration.documents.proxyInput())
        .waitForVisible(PO.registration.documents.proxyAttachGroup())
        // - do: нажать на "Отправить на проверку"
        .click(PO.registration.documents.startModerationButton())
        // - assert: открылась страница модерации
        .yaWaitForPage('/registration/moderation/ongoing')
    );
  });

  /**
   * @see https://testpalm2.yandex-team.ru/testcase/pay-50
   */
  it('Изменить данные во время регистрации', async function() {
    const PO = this.PO;
    const legalData = await this.browser.yaUseFaker(
      makeLegalDataCreator({ withIPType: true })
    );

    return (
      this.browser
        // - do: пройти первый и второй шаги регистрации
        .yaSkipRegistrationInitialStep({ withIPType: true })
        .yaSkipRegistrationLegalStep({ withIPType: true })
        // - do: открыть страницу '/registration/documents'
        .url('/registration/documents')
        // - do: нажать на "Проверить перед отправкой"
        .click(PO.registration.documents.openLegalDataModalButton())
        // - assert: открылось модальное окно с данными мерчанта
        .waitForVisible(PO.registration.legalModal())
        // - screenshot: вид модального окна [registration_legal_modal]
        .assertView(
          'registration_legal_modal',
          PO.registration.legalModal.content(),
          /**
           * Hermione can only scroll the body.
           */
          {
            allowViewportOverflow: true,
            compositeImage: false
          }
        )
        // - do: нажать на "Изменить данные"
        .click(PO.registration.legalModal.changeLegalDataButton())
        // - assert: открылась страница с данными мерчанта
        .waitForVisible(PO.registration.legal())
        // - do: изменить некоторые значения
        .yaClearValue(PO.registration.legal.ceoPersonSurnameInput())
        .setValue(
          PO.registration.legal.ceoPersonSurnameInput(),
          legalData.persons.ceo.surname
        )
        .yaClearValue(PO.registration.legal.ceoPersonNameInput())
        .setValue(
          PO.registration.legal.ceoPersonNameInput(),
          legalData.persons.ceo.name
        )
        // - do: нажать на "Сохранить и продолжить"
        .click(PO.registration.legal.startLegalDataSavingButton())
        // - assert: открылась страница подписания оферты
        .waitForVisible(PO.registration.documents())
        // - do: нажать на "Проверить перед отправкой"
        .click(PO.registration.documents.openLegalDataModalButton())
        // - assert: открылось модальное окно с данными мерчанта
        .waitForVisible(PO.registration.legalModal())
        // - screenshot: вид модального окна [registration_legal_modal_2]
        .assertView(
          'registration_legal_modal_2',
          PO.registration.legalModal.content(),
          /**
           * Hermione can only scroll the body.
           */
          {
            allowViewportOverflow: true,
            compositeImage: false
          }
        )
    );
  });

  /**
   * @see https://testpalm2.yandex-team.ru/testcase/pay-193
   */
  it('Все поля в пререгистрации обязательны', async function() {
    const PO = this.PO;

    return (
      this.browser
        // - do: открыть страницу '/registration/initial'
        .url('/registration/initial')
        // - do: корректно заполнить форму
        .yaDisableAnimations(PO.registration.initial.suggestForInputs())
        .yaWaitForHidden(PO.registration.initial.enabledButton())
        .waitForVisible(PO.registration.initial.yandexBusinessOption())
        .click(PO.registration.initial.yandexBusinessOption())
        .yaWaitForHidden(PO.registration.initial.enabledButton())
        .click(PO.registration.initial.caregotyInput())
        .click(PO.registration.initial.suggestForInputs())
        .yaWaitForHidden(PO.registration.initial.enabledButton())
        .setValue(
          PO.registration.initial.innInput(),
          '5031076070'
        )
        .pause(5000)
        .click(PO.registration.initial.suggestForInputs())
        // - do: нажать на "Следующий шаг"
        .click(PO.registration.initial.enabledButton())
        // - assert: открылась страница legal
        .yaWaitForPage('/registration/legal')
    );
  });
});
