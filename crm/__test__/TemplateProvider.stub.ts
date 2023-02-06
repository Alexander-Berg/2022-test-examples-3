import {
  TemplateProvider,
  TemplateItemAny,
  TemplateFilter,
  TemplateByType,
  Template,
} from 'types/TemplateProvider';

export class TemplateProviderStub implements TemplateProvider {
  static templateStub: Template = {
    id: '1',
    name: 'template name 1',
    bodyHtml: 'bodyHtml',
    bodyPlain: 'bodyPlain',
    isDefault: false,
  };

  getTemplate(_id: string): Template {
    return TemplateProviderStub.templateStub;
  }

  getTemplateListByFilter(_filter: TemplateFilter): TemplateItemAny[] {
    return [TemplateProviderStub.templateStub];
  }

  getTypes(): TemplateByType[] {
    return [
      { id: 'type_1', caption: 'Type 1', itemIds: [] },
      { id: 'type_2', caption: 'Type 2', itemIds: [] },
    ];
  }

  hasTemplate(_id): boolean {
    return false;
  }
}
