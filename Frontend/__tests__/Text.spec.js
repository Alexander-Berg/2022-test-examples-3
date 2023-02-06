import React from 'react';
import renderer from 'react-test-renderer';
import 'jest-styled-components';

import Text from '../';
import {WithTheme} from '../../../test/helpers';

describe('Snapshots', () => {
	test('render Text', () => {
		const component = renderer.create(
			<WithTheme>
				<Text
					align='right'
					textDecoration='underline'
					display='block'
					indent='xxxxl'
					size='l'
					letterSpacing='m'
					fontStyle='italic'
					transform='uppercase'
					type='blockquote'
					view='brand'
					weight='thin'
				>
					Люк, я твой отец
				</Text>
			</WithTheme>
		);
		const json = component.toJSON();
		expect(json).toMatchSnapshot();
	});
});
