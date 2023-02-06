import asyncio

from click.testing import CliRunner

from sendr_cmncommands import commands as cmncommands


def test_shell_context_with_future():
    # sha512('4370757e-061d-448e-b1e3-8798ab2b723f')
    bookmark = (
        '3ad5a5dfddb9f08b3adedfc3fac1c64cfd5ba2b2cccc6a0c51992c70e2fefaacac028cdf81f913c5a029d44aced6bad15410e2e711'
        'c5c01f75845e4a45357725'
    )

    def create_shell_context():
        future = asyncio.Future()
        future.set_result(bookmark)

        return {
            'future': future,
        }

    cli = cmncommands.shell_command(create_shell_context)

    runner = CliRunner()
    ipython_session = """
    import asyncio
    await asyncio.wait_for(future, 0.001)
    exit
    """
    result = runner.invoke(cli, input=ipython_session)

    assert not result.exit_code, result
    assert bookmark in result.output
