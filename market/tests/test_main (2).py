import os
import pytest
from argparse import Namespace

import main
from helpers import source_path, output_path


@pytest.fixture()
def mj_test_service(request, mocker):
    test_path = output_path(os.path.join('results', request.module.__name__ + '-' + request.function.__name__))
    args = Namespace(path=source_path('market/dev-exp/mj-test-service'),
                     idea=test_path,
                     arcadia=source_path(''),
                     generation_folder=None,
                     vcs_add=False,
                     should_call_ya_ide=True,
                     ide_flags='')
    mocker.patch('fill_context.parse_argument', return_value=args)


def test_main(mj_test_service):
    main.main()
