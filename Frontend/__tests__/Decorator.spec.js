import React from 'react';
import renderer from 'react-test-renderer';
import 'jest-styled-components';

import Decorator from '../';
import {WithTheme} from '../../../test/helpers';

describe('Snapshots', () => {
	test('render Decorator', () => {
		const component = renderer.create(
			<WithTheme>
				<Decorator
					spaceT='xxxl'
					spaceH='l'
					spaceR='s'
					spaceB='xxxxxl'
					indentV='m'
					indentA='xxxxxl'
					indentL='xxxxxl'
				>
					Decorator
				</Decorator>
			</WithTheme>
		);
		const json = component.toJSON();
		expect(json).toMatchSnapshot();
	});
});
