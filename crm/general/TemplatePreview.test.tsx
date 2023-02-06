import React from 'react';
import { render } from '@testing-library/react';
import 'components/AttachmentList';
import { TemplatePreview } from './TemplatePreview';

jest.mock('components/AttachmentList', () => ({
  AttachmentListStatefulWithFileViewer: (props) => <div>{JSON.stringify(props)}</div>,
}));

describe('TemplatePreview', () => {
  it('renders only body', () => {
    const { container } = render(<TemplatePreview body={'<div>html</div>'} />);

    expect(container).toMatchSnapshot();
  });

  it('renders all fields', () => {
    const { container } = render(
      <TemplatePreview
        title="title"
        body={'<div>html</div>'}
        files={[{ id: 1, size: 'size', type: 'type', name: 'name', urlName: 'urlName' }]}
      />,
    );

    expect(container).toMatchSnapshot();
  });
});
