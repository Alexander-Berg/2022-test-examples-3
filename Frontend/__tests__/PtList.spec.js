import React from 'react';
import renderer from 'react-test-renderer';
import 'jest-styled-components';

import PtList from '../';
import {WithTheme} from '../../../test/helpers';

describe('Snapshots', () => {
	test('render PtList', () => {
		const component = renderer.create(
			<WithTheme>
				<PtList border='all' shadow='cloud' view='alert'>
					<PtList.Item spaceA='xl' border='top'>Item</PtList.Item>
				</PtList>
			</WithTheme>
		);
		const json = component.toJSON();
		expect(json).toMatchSnapshot();
	});
});
