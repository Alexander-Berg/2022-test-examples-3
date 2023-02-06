package API::Test::Reports::FakeAuthorization;
use Direct::Modern;

use parent 'API::Authorization';

sub new {
    my ( $class, $user ) = @_;
    return $class->SUPER::new(
        request_id => '1',
        remote_address => '127.0.0.1',
        token => '00000000000000000000000000000000',
        application_id => '00000000000000000000000000000000',
        operator_uid => $user->uid,
        operator_user => $user,
        _token_auth_info => {
            uid => $user->uid,
            login => $user->login,
            operator_uid => $user->uid,
            operator_login => $user->login,
        },
    );
}

1;
