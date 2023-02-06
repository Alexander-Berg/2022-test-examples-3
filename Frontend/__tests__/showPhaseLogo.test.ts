import { getServerCtxStub } from 'sport/tests/stubs/contexts/ServerCtx';
import { EPlatform } from 'neo/types/EPlatform';

import { showPhaseLogo } from '../showPhaseLogo';

describe('showPhaseLogo', () => {
  const platform = EPlatform.DESKTOP;
  const flag = `yxneo_sport_${platform}_competition-logo-enabled`;
  const getContext = (flagValue: string) => {
    return getServerCtxStub({
      specialArgs: {
        neo: {
          platform,
          flags: {
            [flag]: flagValue,
          },
        },
      },
    });
  };

  it('В рубрике вида спорта показывется лого турниров', () => {
    const serverCtx = getContext('match-center,tournament-teaser,competition-calendar,team-calendar');
    const result = showPhaseLogo(serverCtx, 'tournament-teaser');

    expect(result).toBeTruthy();
  });

  it('В рубрике вида спорта не показывется лого турниров', () => {
    const serverCtx = getContext('match-center,competition-calendar,team-calendar');
    const result = showPhaseLogo(serverCtx, 'tournament-teaser');

    expect(result).toBeFalsy();
  });
});
