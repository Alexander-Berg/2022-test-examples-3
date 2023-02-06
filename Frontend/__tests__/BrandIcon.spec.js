import React from 'react';
import renderer from 'react-test-renderer';
import 'jest-styled-components';

import BrandLogo from '../index';

describe('Snapshots', () => {
	test('render BrandLogo with size="s"', () => {
		const component = renderer.create(
			<BrandLogo name='yamoney' size='s' />
		);
		const json = component.toJSON();
		expect(json).toMatchSnapshot();
	});

	test('render BrandLogo with size="m"', () => {
		const component = renderer.create(
			<BrandLogo name='absolute' size='m' />
		);
		const json = component.toJSON();
		expect(json).toMatchSnapshot();
	});

	test('render BrandLogo with size="l"', () => {
		const component = renderer.create(
			<BrandLogo name='maryno' size='l' />
		);
		const json = component.toJSON();
		expect(json).toMatchSnapshot();
	});

	test('render BrandLogo with size="xl"', () => {
		const component = renderer.create(
			<BrandLogo name='simtrevel' size='xl' />
		);
		const json = component.toJSON();
		expect(json).toMatchSnapshot();
	});
});
