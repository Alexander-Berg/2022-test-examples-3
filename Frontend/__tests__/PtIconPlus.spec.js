import React from 'react';
import renderer from 'react-test-renderer';
import 'jest-styled-components';

import PtIconPlus from '../';
import {WithTheme} from '../../../test/helpers';

describe('Snapshots', () => {
	test('render PtIconPlus', () => {
		const component = renderer.create(
			<WithTheme>
				<PtIconPlus indent='m' verticalAlign='top'>
					<PtIconPlus.Icon indentRight='s'>Icon</PtIconPlus.Icon>
					<PtIconPlus.Block>Block</PtIconPlus.Block>
				</PtIconPlus>
			</WithTheme>
		);
		const json = component.toJSON();
		expect(json).toMatchSnapshot();
	});

	test('render PtIconPlus', () => {
		const component = renderer.create(
			<WithTheme>
				<PtIconPlus indent='m' verticalAlign='top'>
					<PtIconPlus.Icon indentR='s'>Icon</PtIconPlus.Icon>
					<PtIconPlus.Block>Block</PtIconPlus.Block>
				</PtIconPlus>
			</WithTheme>
		);
		const json = component.toJSON();
		expect(json).toMatchSnapshot();
	});
});
