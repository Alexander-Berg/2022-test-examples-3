package Test::JavaIntapiMocks::BidModifiers;

=head1 NAME

    Test::JavaIntapiMocks::BidModifiers - функции для моков походов за корректировками в java-intapi

=head1 SYNOPSIS

    use Test::JavaIntapiMocks::BidModifiers ':forward_to_perl';

=head1 DESCRIPTION

    Если импортировать тег ':forward_to_perl', будут выставлены глобальные моки на получение и обновление
    корректировок, которые передают управление в соответствующий перловый код.
    Полезно для старых юнит тестов, которые хотят выставить или прочитать корректировки на кампанию.
    Мок остаётся на всю жизнь процесса.

=cut

use Direct::Modern;

use Direct::Validation::HierarchicalMultipliers;
use HierarchicalMultipliers;

sub import {
    my $pkg = shift;

    my %opts = map { ($_ =~ s/^://r) => 1} grep {/^:/} @_;

    my @unknown_opts = grep {!/^(?:forward_to_perl)$/} keys %opts;
    die "Unknown import options: ".join(', ', @unknown_opts) if @unknown_opts;

    if ($opts{forward_to_perl}) {
        no warnings qw/redefine once/;
        *JavaIntapi::UpdateBidModifiers::call = \&_update_bid_modifiers_perl_forward_mock;
        *JavaIntapi::GetBidModifiers::call = \&_get_bid_modifiers_perl_forward_mock;
        *JavaIntapi::ValidateBidModifiers::call = \&_validate_bid_modifiers_perl_forward_mock;
    }

    $pkg->SUPER::import(grep {!/^:/} @_);
}

sub _update_bid_modifiers_perl_forward_mock {
    my ($self) = @_;

    my $multipliers = $self->_prepare();
    my $cid = $self->campaign_id();
    my $pid = $self->adgroup_id();
    my $result =  HierarchicalMultipliers::save_hierarchical_multipliers($cid, $pid, $multipliers, dont_forward_to_java => 1);

    return {
        success => 1,
        affectedResult => $result,
    };
}

sub _get_bid_modifiers_perl_forward_mock {
    my ($self) = @_;

    my $cid = $self->campaign_id();
    return HierarchicalMultipliers::get_hierarchical_multipliers($cid);
}

sub _validate_bid_modifiers_perl_forward_mock {
    my ($self) = @_;

    my $multipliers = $self->_prepare();
    my $campaign_type = $self->campaign_type();
    my $client_id = $self->client_id();
    return Direct::Validation::HierarchicalMultipliers::validate_hierarchical_multipliers(
        $campaign_type,
        $client_id,
        $multipliers,
        dont_forward_to_java => 1,
    );
}


1;
