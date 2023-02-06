package API::Test::Reports::FakeUser;
use Direct::Modern;

=head1 NAME

API::Test::Reports::FakeUser

=head1 DESCRIPTION

Компонент с интерфейсом как у API::Authorization::User, но который с помощью переменных пакета
можно настроить, чтобы у него было поведение, которое нужно в тестах: например, чтобы за соответствием
uid-ClientID он не ходил в базу, а отдавал вручную написанные данные.

=cut

use parent 'API::Authorization::User';

use Guard;

our $NO_REAL_CALLS;

# login => { uid => 123, ClientID => 456 }
our %USER_MAP;

=head2 get_override_guard

Возвращает объект, который при разрушении сбросит переменные пакета, нужные для настройки поведения.

=cut

sub get_override_guard {
    my ($class) = @_;
    return guard {
        %USER_MAP = ();
    };
}

=head2 new

=cut

sub new {
    my ( $class, $user_login ) = @_;

    my ( $uid, $ClientID );
    if ($NO_REAL_CALLS) {
        my $user_data = $USER_MAP{$user_login};

        die "No user data for login = $user_login" unless $user_data;

        $uid = $user_data->{uid};
        $ClientID = $user_data->{ClientID};
    } else {
        require PrimitivesIds;

        $uid = PrimitivesIds::get_uid( login => $user_login );
        $ClientID = PrimitivesIds::get_clientid( login => $user_login );

        $USER_MAP{$user_login} = { uid => $uid, ClientID => $ClientID };
    }

    return $class->SUPER::new( uid => $uid, login => $user_login, ClientID => $ClientID, passport_karma => 0 );
}

1;
