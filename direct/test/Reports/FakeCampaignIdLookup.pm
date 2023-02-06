package API::Test::Reports::FakeCampaignIdLookup;
use Direct::Modern;

use Guard;

our $NO_REAL_CALLS;

# cid => OrderID
our %CID_TO_ORDERID;

=head2 get_override_guard

Перекрыть процедуры, связанные с получением данных по id кампаний в Reports и вернуть объект,
который при разрушении вернёт её обратно.

=cut

sub get_override_guard {
    my ($class) = @_;

    no warnings 'redefine';

    my $orig_get_cid2orderid = \&API::Service::Reports::get_cid2orderid;

    *API::Service::Reports::get_cid2orderid = sub($$) {
        my ( $type, $cids ) = @_;

        die "Invalid type" unless $type eq 'cid';

        if ($NO_REAL_CALLS) {
            my %result;

            for my $cid (@$cids) {
                die "No OrderID for cid = $cid" unless exists $CID_TO_ORDERID{$cid};
                $result{$cid} = $CID_TO_ORDERID{$cid};
            }

            return \%result;
        }

        my $orig_result = $orig_get_cid2orderid->( $type, $cids );
        for my $cid (@$cids) {
            $CID_TO_ORDERID{$cid} = $orig_result->{$cid};
        }

        return $orig_result;
    };

    my $orig_get_uac_oids = \&CampaignTools::mass_get_is_uac_campaign_by_order_ids;

    *API::Service::Reports::mass_get_is_uac_campaign_by_order_ids = sub {
        return {};
    };

    return guard {
        *API::Service::Reports::get_cid2orderid = $orig_get_cid2orderid;
        *API::Service::Reports::mass_get_is_uac_campaign_by_order_ids = $orig_get_uac_oids;
        %CID_TO_ORDERID = ();
    };
}

1;
