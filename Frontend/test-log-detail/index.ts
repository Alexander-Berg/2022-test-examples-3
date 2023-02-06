import { connect } from 'react-redux';
import { bindActionCreators, Dispatch } from 'redux';

import TestLogDetail from 'client/components/submission/test-log-detail/test-log-detail';

import { copyToClipboard } from 'client/store/client/actions';

const mapDispatchToProps = (dispatch: Dispatch) =>
    bindActionCreators(
        {
            copyToClipboard,
        },
        dispatch,
    );

export default connect(null, mapDispatchToProps)(TestLogDetail);
