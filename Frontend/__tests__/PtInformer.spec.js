import React from 'react';
import renderer from 'react-test-renderer';
import 'jest-styled-components';

import PtInformer from '../';
import {WithTheme} from '../../../test/helpers';

describe('Snapshots', () => {
	test('render PtInformer', () => {
		const component = renderer.create(
			<WithTheme>
				<PtInformer border='all' view='warning'>
					<PtInformer.Content>Content</PtInformer.Content>
					<PtInformer.Column>Column-1</PtInformer.Column>
					<PtInformer.Column>Column-2</PtInformer.Column>
					<PtInformer.Action>Action</PtInformer.Action>
				</PtInformer>
			</WithTheme>
		);
		const json = component.toJSON();
		expect(json).toMatchSnapshot();
	});
});
