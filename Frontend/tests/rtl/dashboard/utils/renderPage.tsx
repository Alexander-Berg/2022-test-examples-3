import React from 'react';
import ApplicationsDashboardView from '~/views/ApplicationsDashboardView/ApplicationsDashboardView';
import { DashboardPageFragment } from '../fragments/DashboardPageFragment';
import { IRenderFragmentResult, RenderParams, renderFragment } from '../../utils/renderFragment';

export type DashboardPage = IRenderFragmentResult<DashboardPageFragment>

export async function renderDashboardPage(params: RenderParams): Promise<DashboardPage> {
    return renderFragment(DashboardPageFragment, <ApplicationsDashboardView />, params);
}
