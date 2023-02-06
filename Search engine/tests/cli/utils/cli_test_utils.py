from cli_main import create_main_argparser


def check_cli_line(line: str):
    parser, _ = create_main_argparser()
    arg_list = line.split()
    args, unknown_args = parser.parse_known_args(arg_list)
    assert not unknown_args, f"Unexpected arguments: {unknown_args}"
    return args
