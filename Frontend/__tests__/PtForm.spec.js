import React from 'react';
import renderer from 'react-test-renderer';
import 'jest-styled-components';

import PtForm from '../';
import {WithTheme} from '../../../test/helpers';

describe('Snapshots', () => {
	test('render PtForm', () => {
		const component = renderer.create(
			<WithTheme>
				<PtForm
					view='default'
					border='all'
					shadow='cloud'
				>
					<PtForm.Control>
						<PtForm.Item spaceA='xl' border='top'>Item - 1</PtForm.Item>
						<PtForm.Item spaceA='s' border='bottom'>Item - 2</PtForm.Item>
						<PtForm.Label width='inverse'>Ya veseliy label</PtForm.Label>
					</PtForm.Control>
				</PtForm>
			</WithTheme>
		);
		const json = component.toJSON();
		expect(json).toMatchSnapshot();
	});

	test('render PtForm.Item without vertical align', () => {
		const component = renderer.create(
			<WithTheme>
				<PtForm.Item spaceA='xl' border='top'>Item - 1</PtForm.Item>
			</WithTheme>
		);
		const json = component.toJSON();
		expect(json).toMatchSnapshot();
	});

	test('render PtForm.Item without vertical align `default`', () => {
		const component = renderer.create(
			<WithTheme>
				<PtForm.Item spaceA='xl' border='top' verticalAlign="default">Item - 1</PtForm.Item>
			</WithTheme>
		);
		const json = component.toJSON();
		expect(json).toMatchSnapshot();
	});

	test('render PtForm.Item without vertical align `center`', () => {
		const component = renderer.create(
			<WithTheme>
				<PtForm.Item spaceA='xl' border='top' verticalAlign="center">Item - 1</PtForm.Item>
			</WithTheme>
		);
		const json = component.toJSON();
		expect(json).toMatchSnapshot();
	});

	test('render PtForm.Item without vertical align `baseline`', () => {
		const component = renderer.create(
			<WithTheme>
				<PtForm.Item spaceA='xl' border='top' verticalAlign="baseline">Item - 1</PtForm.Item>
			</WithTheme>
		);
		const json = component.toJSON();
		expect(json).toMatchSnapshot();
	});
});
