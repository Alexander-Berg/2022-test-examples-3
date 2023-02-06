import React from 'react';
import renderer from 'react-test-renderer';
import 'jest-styled-components';

import ServiceLogo from '../index';

describe('Snapshots', () => {
	test('render ServiceLogo with size="s"', () => {
		const component = renderer.create(
			<ServiceLogo name='afisha' size='s' />
		);
		const json = component.toJSON();
		expect(json).toMatchSnapshot();
	});

	test('render ServiceLogo with size="m"', () => {
		const component = renderer.create(
			<ServiceLogo name='afisha' size='m' />
		);
		const json = component.toJSON();
		expect(json).toMatchSnapshot();
	});

	test('render ServiceLogo with size="l"', () => {
		const component = renderer.create(
			<ServiceLogo name='afisha' size='l' />
		);
		const json = component.toJSON();
		expect(json).toMatchSnapshot();
	});

	test('render ServiceLogo with size="xl"', () => {
		const component = renderer.create(
			<ServiceLogo name='afisha' size='xl' />
		);
		const json = component.toJSON();
		expect(json).toMatchSnapshot();
	});
});
