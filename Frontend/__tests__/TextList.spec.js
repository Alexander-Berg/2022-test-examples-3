import React from 'react';
import renderer from 'react-test-renderer';
import 'jest-styled-components';

import TextList from '../';
import {WithTheme} from '../../../test/helpers';

describe('Snapshots', () => {
	test('render TextList', () => {
		const component = renderer.create(
			<WithTheme>
				<TextList type='bullet'>
					<TextList.Item>1</TextList.Item>
					<TextList.Item>2</TextList.Item>
					<TextList.Item>3</TextList.Item>
				</TextList>
			</WithTheme>
		);
		const json = component.toJSON();
		expect(json).toMatchSnapshot();
	});
});
