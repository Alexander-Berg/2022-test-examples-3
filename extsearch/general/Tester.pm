package FastRes::Tester;

use common::sense;

use FastRes;
use FastRes::Tester::Rules;
use FastRes::Tester::SerpData;
use FastRes::Tester::Requests;

use Data::Dumper;

sub new {
    my ($class) = @_;

    my $self = {
        'rules'     => FastRes::Tester::Rules->new(),
        'serpdata'  => FastRes::Tester::SerpData->new(),
        'requests'  => FastRes::Tester::Requests->new(),
        'force'     => 0,
    };

    return bless $self, ref($class) || $class;
}

sub verify_syntax {
    my ($self, $obj, $params) = @_;
    my $res;

    if ($params->{meta}) {
        $res->{success}{meta} = 1;
        $self->{force} = 1;
    }
    if ($params->{wizard}) {
        if ($obj->{rules} && keys %{$obj->{rules}}) {
            $res->{success}{wizard} //= 1;
            $res->{rules} = $self->_verify(object => $obj, component => 'rules');
            if (@{$res->{rules}{errors}}) {
                $res->{errors} = 1;
                $res->{success}{wizard} = 0;
            }
            else {
                $obj->{markers}{verified} = 1; #save as verified too
            }
        }
        if ($obj->{requests} && ref($obj->{requests}) eq 'HASH' && keys %{$obj->{requests}}) {
            $res->{success}{wizard} //= 1;
            $res->{requests} = $self->_verify(object => $obj, component => 'requests');
            if (@{$res->{requests}{errors}}) {
                $res->{errors} = 1;
                $res->{success}{wizard} = 0;
            }
        }
    }
    if ($params->{data}) {
        if ($obj->{serpdata} && keys %{$obj->{serpdata}}) {
            $res->{success}{data} //= 1;
            $res->{serpdata} = $self->_verify(object => $obj, component => 'serpdata');
            if (@{$res->{serpdata}{errors}}) {
                $res->{errors} = 1;
                $res->{success}{data} = 0;
            }
        }
        if ($obj->{relev_rearr} && ref($obj->{relev_rearr}) eq 'HASH' && keys %{$obj->{relev_rearr}}) {
            $res->{success}{data} //= 1;
            $res->{relev_rearr}{data} = '';
            # TODO: checks
            $obj->{relev_rearr}{verified} = 1;
        }
    }
    return $res;
}


sub _verify {
    my ($self, %params) = @_;

    my ($obj, $component) = @params{qw/object component/};
    my $res;

    die "Undefined object to verify" unless $obj;
    die "Don't know what to verify (undefined component)" unless $component;

    if ($obj->{$component}{verified} && !$self->{force}) {
        $res->{already} = 1;
        return $res;
    }
    else {
        $res = $self->{$component}->verify_syntax($obj);
        $obj->{$component}{verified} = 1 unless (@{$res->{errors}});
    }
    return $res;
}

# TODO: common input point
=cut
sub sandbox_testing {
    my ($self, %params) = @_;

    if ($self->{$params{component}}->can('sandbox_testing')) {
        return $self->{$params{component}}->sandbox_testing(%params);
    }
    else {
        return { errors => [ "Component '$params{component}' has no method for sandbox testing" ] };
    }
}
=cut

1;
