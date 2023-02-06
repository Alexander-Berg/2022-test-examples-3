import { MailTemplateProvider } from './MailTemplateProvider';
import { MailTemplateGroup } from './MailTemplateProvider.types';

describe('MailTemplateProvider', () => {
  describe('when empty array', () => {
    describe('.getTemplate', () => {
      it('throws error', () => {
        const mailTemplateProvider = new MailTemplateProvider([]);

        expect(() => {
          mailTemplateProvider.getTemplate('id');
        }).toThrow(/Has no template with id/i);
      });
    });

    describe('.getTemplateListByFilter', () => {
      it('returns empty array', () => {
        const mailTemplateProvider = new MailTemplateProvider([]);

        expect(mailTemplateProvider.getTemplateListByFilter({ typeId: 'type' })).toStrictEqual([]);
      });
    });

    describe('.hasTemplate', () => {
      it('returns false', () => {
        const mailTemplateProvider = new MailTemplateProvider([]);

        expect(mailTemplateProvider.hasTemplate('id')).toBe(false);
      });
    });

    describe('.getTypes', () => {
      it('returns empty array', () => {
        const mailTemplateProvider = new MailTemplateProvider([]);

        expect(mailTemplateProvider.getTypes()).toStrictEqual([]);
      });
    });
  });

  describe('when non empty array', () => {
    const inputTemplatesFormat: MailTemplateGroup[] = [
      {
        id: 'type_1',
        caption: 'caption_1',
        items: [
          {
            id: 1,
            bodyHtml: 'bodyHtml_1',
            bodyPlain: 'bodyPlain_1',
            isDefault: false,
            type: 'group_1',
            typeName: 'groupName_1',
          },
        ],
      },
      {
        id: 'type_2',
        caption: 'caption_2',
        items: [
          {
            id: 2,
            bodyHtml: 'bodyHtml_2',
            bodyPlain: 'bodyPlain_2',
            isDefault: false,
            type: 'group_1',
            typeName: 'groupName_1',
          },
        ],
      },
    ];

    describe('.getTemplate', () => {
      it('returns template', () => {
        const mailTemplateProvider = new MailTemplateProvider(inputTemplatesFormat);

        expect(mailTemplateProvider.getTemplate('1')).toMatchSnapshot();
      });
    });

    describe('.getTemplateListByFilter', () => {
      describe('filter by not exist type', () => {
        it('returns empty array', () => {
          const mailTemplateProvider = new MailTemplateProvider(inputTemplatesFormat);

          expect(
            mailTemplateProvider.getTemplateListByFilter({ typeId: 'type_not_exist' }),
          ).toMatchSnapshot();
        });
      });

      describe('filter by type', () => {
        it('returns result', () => {
          const mailTemplateProvider = new MailTemplateProvider(inputTemplatesFormat);

          expect(
            mailTemplateProvider.getTemplateListByFilter({ typeId: 'type_1' }),
          ).toMatchSnapshot();
        });
      });

      describe('filter by type and text', () => {
        it('returns result', () => {
          const mailTemplateProvider = new MailTemplateProvider(inputTemplatesFormat);

          expect(
            mailTemplateProvider.getTemplateListByFilter({ typeId: 'type_1', text: 'bodyHtml_1' }),
          ).toMatchSnapshot();
        });
      });

      describe('filter by not match text', () => {
        it('returns empty array', () => {
          const mailTemplateProvider = new MailTemplateProvider(inputTemplatesFormat);

          expect(
            mailTemplateProvider.getTemplateListByFilter({
              typeId: 'type_1',
              text: 'bodyHtml_1_2',
            }),
          ).toMatchSnapshot();
        });
      });
    });

    describe('.hasTemplate', () => {
      it('returns true', () => {
        const mailTemplateProvider = new MailTemplateProvider(inputTemplatesFormat);

        expect(mailTemplateProvider.hasTemplate('2')).toBe(true);
      });
    });

    describe('.getTypes', () => {
      it('returns types', () => {
        const mailTemplateProvider = new MailTemplateProvider(inputTemplatesFormat);

        expect(mailTemplateProvider.getTypes()).toMatchSnapshot();
      });
    });
  });
});
