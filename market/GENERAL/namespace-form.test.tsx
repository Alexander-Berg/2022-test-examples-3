import React from 'react';
import { fireEvent, render } from '@testing-library/react';

import { NamespaceForm } from '@/pages/schema-editor/components/top-menu/namespace-selector/namespace-form/namespace-form';

describe('<NamespaceForm />', () => {
  it('should submit correct values', async () => {
    const onCreate = jest.fn();
    const app = render(<NamespaceForm onCreate={onCreate} namespaceList={[]} />);
    const systemNameInput = await app.findByPlaceholderText('Системное имя');
    const serviceInput = await app.findByPlaceholderText('Сервис');
    const platformInput = await app.findByPlaceholderText('Платформа');
    const enableEditorCheckbox = await app.findByText('Включить редактор схемы');
    const submitButton = await app.findByText('Сформировать');

    fireEvent.change(systemNameInput, { target: { value: 'Testik' } });
    fireEvent.change(serviceInput, { target: { value: 'Testovich' } });
    fireEvent.change(platformInput, { target: { value: 'Testinberg' } });
    fireEvent.click(submitButton);

    expect(onCreate).toBeCalledTimes(1);
    expect(onCreate).toHaveBeenLastCalledWith({
      name: 'Testik',
      service: 'Testovich',
      platform: 'Testinberg',
      disableSchemaEditor: false,
    });

    fireEvent.click(enableEditorCheckbox);
    fireEvent.click(submitButton);
    expect(onCreate).toHaveBeenLastCalledWith({
      name: 'Testik',
      service: 'Testovich',
      platform: 'Testinberg',
      disableSchemaEditor: true,
    });
  });
});
