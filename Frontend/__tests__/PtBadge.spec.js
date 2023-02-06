import React from 'react';
import renderer from 'react-test-renderer';
import 'jest-styled-components';

import PtBadge, {getViewStyles} from '../';
import {WithTheme} from '../../../test/helpers';

/** Поддерживаемые view */
const VIEWS = [
	'success',
	'warning',
	'alert',
	'default',
	'inverse'
];

describe('Snapshots', () => {
	test('render PtBadge', () => {
		const component = renderer.create(
			<WithTheme>
				<PtBadge view='alert'>
					PtBadge
				</PtBadge>
			</WithTheme>
		);
		const json = component.toJSON();
		expect(json).toMatchSnapshot();
	});
});

describe('getViewStyles', () => {
	test('correctly calculates styles by view', () => {
		VIEWS.forEach((view) => {
			const stylesByView = getViewStyles(view);
			expect(stylesByView).toMatchSnapshot();
		});
	});
});
