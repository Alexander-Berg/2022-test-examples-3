/**
 * @typedef {import('../commands/ya-use-faker').Faker} Faker
 */

/**
 * @typedef {Object} OrganizationData
 * @property {string} type
 * @property {string} name
 * @property {string} fullName
 * @property {string} englishName
 * @property {string} ogrn
 * @property {string} inn
 * @property {string|void} kpp
 * @property {string} scheduleText
 * @property {string} siteUrl
 * @property {string} description
 */

/**
 * @typedef {Object} PersonData
 * @property {string} name
 * @property {string} surname
 * @property {string} birthDate
 * @property {string} phone
 * @property {string} email
 */

/**
 * @typedef {Object} PersonsData
 * @property {PersonData} ceo
 * @property {PersonData|void} contact
 */

/**
 * @typedef {Object} AddressData
 * @property {string} country
 * @property {string} city
 * @property {string} street
 * @property {string} home
 * @property {string} zip
 */

/**
 * @typedef {Object} AddressesData
 * @property {AddressData} legal
 * @property {AddressData|void} post
 */

/**
 * @typedef {Object} BankData
 * @property {string} name
 * @property {string} correspondentAccount
 * @property {string} account
 * @property {string} bik
 */

/**
 * @typedef {Object} LegalData
 * @property {OrganizationData} organization
 * @property {PersonsData} persons
 * @property {AddressesData} addresses
 * @property {BankData} bank
 * @property {string} name
 * @property {string} username
 */

/**
 * @function
 * @param {Faker} faker
 * @returns {string}
 */
const createBirthDate = faker => {
  const date = faker.date.past(20, '2000-01-01T00:00:00.000Z');
  const year = date.getFullYear();
  const month = String(date.getMonth() + 1).padStart(2, '0');
  const day = String(date.getDate()).padStart(2, '0');

  return `${day}.${month}.${year}`;
};

/**
 * @function
 * @param {Faker} faker
 * @returns {Object}
 */
const createCommonOrganizationData = faker => {
  return {
    scheduleText: 'вт–пт: 08:00–20:00; cб: 08:00–18:00; вс: выходной',
    siteUrl: faker.internet.url(),
    description: faker.lorem.sentence(10)
  };
};

/**
 * @function
 * @param {Faker} faker
 * @param {PersonData} person
 * @returns {OrganizationData}
 */
const createOOOOrganizationData = (faker, person) => {
  const companyName = `${person.surname} ${faker.company.companySuffix()}`;

  return {
    ...createCommonOrganizationData(faker),
    type: 'ooo',
    name: companyName,
    fullName: `ООО ${companyName}`,
    englishName: 'abc',
    ogrn: '1075031006368',
    inn: '5031076070',
    kpp: '503101001'
  };
};

/**
 * @function
 * @param {Faker} faker
 * @param {PersonData} person
 * @returns {OrganizationData}
 */
const createIPOrganizationData = (faker, person) => {
  return {
    ...createCommonOrganizationData(faker),
    type: 'ip',
    name: `ИП ${person.surname} ${person.name[0]}.`,
    fullName: `Индивидуальный предприниматель ${person.surname} ${person.name}`,
    englishName: 'abc',
    ogrn: '319784700341752',
    inn: '510105936358'
  };
};

/**
 * @function
 * @param {Faker} faker
 * @returns {PersonData}
 */
const createPersonData = faker => {
  const gender = faker.random.number({ min: 0, max: 1 });

  return {
    name: faker.name.firstName(gender),
    surname: faker.name.lastName(gender),
    patronymic: '',
    birthDate: createBirthDate(faker),
    phone: faker.phone.phoneNumber('+7 9## ### ## ##'),
    email: faker.internet.exampleEmail(
      faker.company.bsAdjective(),
      faker.company.bsNoun()
    )
  };
};

/**
 * @function
 * @returns {AddressData}
 */
const createAddressData = () => {
  /**
   * Адрес должен быть настоящим.
   */
  return {
    country: 'RUS',
    city: 'г. Москва',
    street: 'ул. Льва Толстого',
    home: 'д. 16',
    zip: '119021'
  };
};

/**
 * @function
 * @param {Faker} faker
 * @returns {BankData}
 */
const createBankData = faker => {
  const bankName = faker.helpers.randomize(
    faker.definitions.finance.account_type
  );

  return {
    name: `${bankName} Bank`,
    correspondentAccount: '1234567899',
    account: '40702810700190000201',
    bik: '044583503'
  };
};

/**
 * @function
 * @property {Object} options
 * @property {string} options.userLogin
 * @property {boolean} options.withIPType
 * @property {boolean} [options.withContactPerson]
 * @property {boolean} [options.withPostAddress]
 * @returns {(faker: Faker) => LegalData}
 */
const makeLegalDataCreator = ({
  userLogin,
  withIPType,
  withContactPerson = false,
  withPostAddress = false
}) => faker => {
  const organizationDataCreator = withIPType
    ? createIPOrganizationData
    : createOOOOrganizationData;
  const ceoPerson = createPersonData(faker);
  const organizationData = organizationDataCreator(faker, ceoPerson);

  return {
    organization: organizationData,
    persons: {
      ceo: ceoPerson,
      contact: withContactPerson ? createPersonData(faker) : undefined
    },
    addresses: {
      legal: createAddressData(),
      post: withPostAddress ? createAddressData() : undefined
    },
    bank: createBankData(faker),
    name: organizationData.fullName,
    username: userLogin
  };
};

module.exports = {
  makeLegalDataCreator
};
