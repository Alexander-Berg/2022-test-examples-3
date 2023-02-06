package API::Test::Reports::FakeRBAC;
use Direct::Modern;

use Guard;

our $NO_REAL_CALLS;

# cid => {0|1}
our %ALLOW_SHOW_STAT_CAMPS;

=head2 get_override_guard

Перекрыть процедуру API::Service::Reports::rbac_check_allow_show_stat_camps и вернуть объект,
который при разрушении вернёт её обратно.

=cut

sub get_override_guard {
    my ($class) = @_;

    no warnings 'redefine';

    my $orig_rbac_check_allow_show_camps = \&API::Service::Reports::rbac_check_allow_show_camps;

    *API::Service::Reports::rbac_check_allow_show_camps = sub {
        my ( $rbac, $UID, $cids ) = @_;

        if ($NO_REAL_CALLS) {
            return { map { $_ => $ALLOW_SHOW_STAT_CAMPS{$_} ? 1 : 0 } @$cids };
        }

        my $orig_result = $orig_rbac_check_allow_show_camps->( $rbac, $UID, $cids );
        for my $cid (@$cids) {
            $ALLOW_SHOW_STAT_CAMPS{$cid} = $orig_result->{$cid} ? 1 : 0;
        }

        return $orig_result;
    };

    return guard {
        *API::Service::Reports::rbac_check_allow_show_camps = $orig_rbac_check_allow_show_camps;
        %ALLOW_SHOW_STAT_CAMPS = ();
    };
}

1;
