import React from 'react';
import renderer from 'react-test-renderer';
import 'jest-styled-components';

import PtFlex from '../';
import {WithTheme} from '../../../test/helpers';

describe('Snapshots', () => {
	test('render PtFlex', () => {
		const component = renderer.create(
			<WithTheme>
				<PtFlex
					view='phantom'
					border='all'
					shadow='cloud'
					status='success'
					spaceV='m'
					spaceH='l'
				>
					<PtFlex.Column width='30' align='right'>Column</PtFlex.Column>
				</PtFlex>
			</WithTheme>
		);
		const json = component.toJSON();
		expect(json).toMatchSnapshot();
	});
});
