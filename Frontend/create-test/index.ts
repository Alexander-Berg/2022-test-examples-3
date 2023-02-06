import { connect } from 'react-redux';
import { Omit } from 'utility-types';

import CreateTest from 'client/components/create-test/create-test';
import { Props } from 'client/components/create-test/types';
import withId from 'client/decorators/with-id';
import { Props as WithIdProps } from 'client/decorators/with-id/types';
import withPopup from 'client/decorators/with-popup';
import { Props as WithPopupProps } from 'client/decorators/with-popup/types';
import { selectCreateTestError, selectCreateTestStarted } from 'client/selectors/problems';
import { createTest } from 'client/store/problems/actions';
import { RootState } from 'client/store/types';

const mapStateToProps = (state: RootState) => ({
    createTestStarted: selectCreateTestStarted(state),
    createTestError: selectCreateTestError(state),
});

const mapDispatchToProps = {
    createTest: createTest.request,
};

type WithoutIdProps = Omit<Props, keyof WithIdProps>;
const CreateTestWithId = withId<WithoutIdProps>(CreateTest);
const CreateTestComposed = withPopup<Omit<WithoutIdProps, keyof WithPopupProps>>()(
    CreateTestWithId,
);

export default connect(mapStateToProps, mapDispatchToProps)(CreateTestComposed);
