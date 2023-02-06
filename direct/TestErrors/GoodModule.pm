package TestErrors::GoodModule;

use Direct::Errors;

error ReqField => (code => 10, text => 'The field is required');

warning ReqField => (code => 10, text => '');

# different type of errors with the same code
error BadRequest => (code => 55, text => '');
warning NoPhraseId => (code => 55, text => '');

# aliases of the same error
error NoRequest => (
    code        => 200_204,
    text        => 'ZZ',
    description => 'QQ',
    suffixes    => {
        A   => 'WW',
        AB  => 'EE',
        ABC => 'RR'
    },
);

1;