import React from 'react';
import renderer from 'react-test-renderer';
import 'jest-styled-components';

import PtTag, {getStylesByView} from '../';
import {WithTheme} from '../../../test/helpers';

/** Поддерживаемые view */
const VIEWS = [
	'default',
	'disable',
	'inverse',
	'link'
];

describe('Snapshots', () => {
	test('render PtTag', () => {
		const component = renderer.create(
			<WithTheme>
				<PtTag />
			</WithTheme>
		);
		const json = component.toJSON();
		expect(json).toMatchSnapshot();
	});
});

describe('getStylesByView', () => {
	test('correctly calculates styles by view', () => {
		VIEWS.forEach((view) => {
			const stylesByView = getStylesByView({view});
			expect(stylesByView).toMatchSnapshot();
		});
	});
});
