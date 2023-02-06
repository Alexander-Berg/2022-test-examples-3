import React from 'react';
import renderer from 'react-test-renderer';
import 'jest-styled-components';

import PtTable from '../';
import {WithTheme} from '../../../test/helpers';

describe('Snapshots', () => {
	test('render PtTable', () => {
		const component = renderer.create(
			<WithTheme>
				<PtTable view='success' border='all'>
					<PtTable.Row view='head'>First row</PtTable.Row>
					<PtTable.Row border='bottom'>Second row with bottom border</PtTable.Row>
					<PtTable.Row>Third Row</PtTable.Row>
					<PtTable.Row stripe='even'>Fourth Row</PtTable.Row>
					<PtTable.Row spaceA='xl' status='success'>
						<PtTable.Column width='30' align='center'>
							Column1 with all sides padding
						</PtTable.Column>
					</PtTable.Row>
				</PtTable>
			</WithTheme>
		);
		const json = component.toJSON();
		expect(json).toMatchSnapshot();
	});
});
