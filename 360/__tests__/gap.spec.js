import {getActiveGap} from 'utils/gap';
import WorkMods from 'constants/WorkMods';
import {Gaps} from 'constants/Gaps';

describe('gap', () => {
  test('должен возвращать gap удаленного режима работы, если передан workMode remote', () => {
    const activeGap = getActiveGap([], WorkMods.REMOTE, false);
    expect(activeGap.type).toBe(Gaps.REMOTE_WORK);
  });

  test('должен возвращать gap удаленного режима работы, если передан workMode mixed', () => {
    const activeGap = getActiveGap([], WorkMods.MIXED, false);
    expect(activeGap.type).toBe(Gaps.REMOTE_WORK);
  });

  test('не должен возвращать ничего, если передан workMode mixed и isMixedAsRemote', () => {
    const activeGap = getActiveGap([], WorkMods.MIXED, true);
    expect(activeGap).toBe(null);
  });

  test('не должен возвращать ничего, если найден gap с работой из офиса', () => {
    const activeGap = getActiveGap(
      [{type: Gaps.OFFICE_WORK}, {type: Gaps.VACATION}],
      WorkMods.OFFICE,
      false
    );
    expect(activeGap).toBe(null);
  });

  test('не должен возвращать ничего, если все gap – дежурство', () => {
    const activeGap = getActiveGap([{type: Gaps.DUTY}], WorkMods.OFFICE, false);
    expect(activeGap).toBe(null);
  });

  test('должен работать первый валидный gap', () => {
    const activeGap = getActiveGap(
      [{type: Gaps.LEARNING}, {type: Gaps.MATERNITY}],
      WorkMods.OFFICE,
      false
    );
    expect(activeGap.type).toBe(Gaps.LEARNING);
  });
});
