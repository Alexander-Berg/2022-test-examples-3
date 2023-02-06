import { shallow } from 'enzyme';

import { SpecialOrderRowFormatterTicket } from 'src/pages/replenishment/components/SpecialOrderTable/utils/formatters';
import { SpecialOrderDTO } from 'src/java/definitions-replenishment';

describe('SpecialOrderTable formatters', () => {
  it('SpecialOrderRowFormatterTicket', () => {
    expect(shallow(SpecialOrderRowFormatterTicket({ ticketId: 'FOO-42' } as SpecialOrderDTO)!).html()).toBe(
      '<a target="_blank" href="https://st.yandex-team.ru/FOO-42" class="Link Link_theme_normal">FOO-42</a>'
    );

    expect(shallow(SpecialOrderRowFormatterTicket({ ticketId: undefined } as SpecialOrderDTO)!).html()).toBe(
      '<span>-</span>'
    );
  });
});
