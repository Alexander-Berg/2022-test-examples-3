package API::Test::Reports::FakeService::Reports;
use Direct::Modern;

=head1 NAME

API::Test::Reports::FakeService::Reports

=head1 DESCRIPTION

Наследник API::Service::Reports, чтобы переопределить у него несколько методов и сделать
им поведение, нужное в тестах, а потом вызвать create, который их позовёт.

Поведение, нужное в тестах, настраивается переменными пакета.

=cut

use parent 'API::Service::Reports';

use Guard;
use HTTP::Headers;

use Yandex::ListUtils;

use API::Test::Reports::FakeAuthorization;
use API::Test::Reports::FakeUser;

our $NO_REAL_CALLS;

our @UNAVAILABLE_CIDS;
our $SUBLCIENT_CURRENCY;
our $CIDS_WITH_STATISTICS;

=head2 get_override_guard

Возвращает объект, который при разрушении сбрасывает переменные пакета, нужные
для настройки поведения в тестах.

=cut

sub get_override_guard {
    my ($class) = @_;
    return guard {
        @UNAVAILABLE_CIDS = ();
        $SUBLCIENT_CURRENCY = undef;
        $CIDS_WITH_STATISTICS = undef;
    };
}

=head2 new

=cut

sub new {
    my ( $class, $user_login, $http_headers ) = @_;

    my $user = API::Test::Reports::FakeUser->new($user_login);
    my $auth = API::Test::Reports::FakeAuthorization->new($user);

    my $self = $class->SUPER::new('Reports');
    $self->set_current_operation('create');
    $self->set_subclient($user);
    $self->set_authorization($auth);
    $self->set_http_request_headers( HTTP::Headers->new( %{ $http_headers || {} } ) );

    return $self;
}

=head2 get_unavailable_cids_map

=cut

sub get_unavailable_cids_map {
    my ( $self, @campaign_ids ) = @_;

    my %unavailable_cids = map { $_ => 1 } @UNAVAILABLE_CIDS;
    return grep { $unavailable_cids{$_} } @campaign_ids;
}

=head2 _get_accessible_client_cids_with_stats

=cut

sub _get_accessible_client_cids_with_stats {
    my ($self, $cids, $campaign_types) = @_;

    if ($NO_REAL_CALLS) {
        if ($cids && @$cids) {
            return $cids;
        }
        die "CIDS_WITH_STATISTICS not set" unless defined $CIDS_WITH_STATISTICS;
        return [ @$CIDS_WITH_STATISTICS ];
    }

    my $result_cids = $self->SUPER::_get_accessible_client_cids_with_stats($cids, $campaign_types);
    $cids = [ nsort @$result_cids ];

    $CIDS_WITH_STATISTICS = [ @$result_cids ];
    return $result_cids;
}

=head2 _get_subcampaigns

=cut

sub _get_subcampaigns {
    my ($self, $cids) = @_;

    if ($NO_REAL_CALLS) {
        return [];
    }

    return [ nsort @{ $self->SUPER::_get_subcampaigns($cids) } ];
}

=head2 _tsv_line

=cut

sub _tsv_line {
    my ($self, $values) = @_;
    return $values;
}

=head2 _get_subclient_currency

=cut

sub _get_subclient_currency {
    my ($self) = @_;

    if ($NO_REAL_CALLS) {
        die "SUBLCIENT_CURRENCY not set" unless defined $SUBLCIENT_CURRENCY;
        return $SUBLCIENT_CURRENCY;
    }

    $SUBLCIENT_CURRENCY = $self->SUPER::_get_subclient_currency;
    return $SUBLCIENT_CURRENCY;
}

=head2 rbac

=cut

sub rbac {
    my ($self) = @_;

    if ($NO_REAL_CALLS) {
        return undef;
    }

    return $self->SUPER::rbac;
}

1;
