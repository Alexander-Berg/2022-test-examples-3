import React from 'react';
import renderer from 'react-test-renderer';
import 'jest-styled-components';

import PtAvatar from '../';

describe('Snapshots', () => {
	test('render PtAvatar', () => {
		const component = renderer.create(
			<PtAvatar>
				<PtAvatar.Photo size='m' />
			</PtAvatar>
		);
		const json = component.toJSON();
		expect(json).toMatchSnapshot();
	});
});
