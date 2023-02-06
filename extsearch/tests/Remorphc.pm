package FastRes::Tests::Remorphc;

use common::sense;

use FastRes::Tests::Common;
use base qw/FastRes::Tests::Common/;

sub new {
    my ($class, $data) = @_;
    
    $data->{src_suffix} = '.rmt';
    $data->{dst_suffix} = '.bin';

    my $self = bless {}, $class;

    $self->_init($data);

    return $self;
}

sub _make_command {
    my ($self, $params) = @_;
    return undef unless ($params->{bin_path} && $params->{gzt_path});

    my @args = ($params->{bin_path}.'/remorphc',
                '-t', 'tokenlogic',
                '-o', $self->{dst},
                '-g', $params->{gzt_path},
                '-v', '2',
                $self->{src},
                '2>&1' );
    $self->{command} = join(' ', @args);
}

sub analyze_errors {
    my $self = shift;

    return undef if ($self->{result} =~ /^\s*$/);
    return $self->{result};
}

1;
