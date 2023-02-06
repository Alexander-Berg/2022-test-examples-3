import React from 'react';
import renderer from 'react-test-renderer';
import 'jest-styled-components';

import TplGrid from '../';
import {WithTheme} from '../../../test/helpers';

describe('Snapshots', () => {
	test('render PtTable', () => {
		const component = renderer.create(
			<WithTheme>
				<TplGrid themeGap='medium' colGap='full' rowGap='full' columns='12' ratio='1-1-1-1-1-1'>
					<TplGrid.Fraction xlCol='6' lCol='6' mCol='6' sCol='6' col='6' row='2'>
						Тест фрактиона
					</TplGrid.Fraction>
					<TplGrid.Fraction xlCol='6' lCol='6' mCol='6' sCol='6' col='6' row='2'>
						Тест фрактиона
					</TplGrid.Fraction>
				</TplGrid>
			</WithTheme>
		);
		const json = component.toJSON();
		expect(json).toMatchSnapshot();
	});
});
