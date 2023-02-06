import React from 'react';
import renderer from 'react-test-renderer';
import 'jest-styled-components';

import PtCard from '../';
import {WithTheme} from '../../../test/helpers';

describe('Snapshots', () => {
	test('render PtCard', () => {
		const component = renderer.create(
			<WithTheme>
				<PtCard
					view='default'
				>
					<PtCard.Image>
						<PtCard.Play>Play</PtCard.Play>
					</PtCard.Image>
					<PtCard.Header>Header</PtCard.Header>
					<PtCard.Content>Content</PtCard.Content>
					<PtCard.Footer>Footer</PtCard.Footer>
				</PtCard>
			</WithTheme>
		);
		const json = component.toJSON();
		expect(json).toMatchSnapshot();
	});

	test('render PtCard', () => {
		const component = renderer.create(
			<WithTheme>
				<PtCard
					view='default'
					border='all'
					shadow='cloud'
				>
					<PtCard.Image
						size='cover'
						shadow='bottom-default'
					>
						<PtCard.Play>Play</PtCard.Play>
					</PtCard.Image>
					<PtCard.Header
						spaceV='xl'
						align='center'
					>
						Header
					</PtCard.Header>
					<PtCard.Content
						align='right'
						verticalAlign='center'
						spaceV='xl'
					>
						Content
					</PtCard.Content>
					<PtCard.Footer
						spaceV='xl'
						align='center'
						distribute='between'
					>
						Footer
					</PtCard.Footer>
				</PtCard>
			</WithTheme>
		);
		const json = component.toJSON();
		expect(json).toMatchSnapshot();
	});
});
