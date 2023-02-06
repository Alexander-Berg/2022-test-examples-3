import { mount } from 'enzyme';
import * as React from 'react';
import { createApiMock } from '@yandex-market/mbo-test-utils';

import { AliasMaker } from 'src/shared/services';
import { ModerationApp } from 'src/tasks/mapping-moderation/components/ModerationApp/ModerationApp';
import { MappingModerationInput, MappingModerationOutput } from 'src/tasks/mapping-moderation/helpers/input-output';

type MmSubmitHandler = () => Promise<MappingModerationOutput | undefined>;

export const setupTestApplication = (input: MappingModerationInput) => {
  const aliasMaker = createApiMock<AliasMaker>();
  const submitHolder = { submit: null as unknown as MmSubmitHandler };
  const onSubmit = (handler: MmSubmitHandler) => {
    submitHolder.submit = handler;
  };
  const app = mount(<ModerationApp input={input} onSubmit={onSubmit} aliasMaker={aliasMaker} />);

  return { aliasMaker, app, submitHolder };
};
