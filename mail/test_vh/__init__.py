import typing
import vh3


@vh3.decorator.external_operation("https://nirvana.yandex-team.ru/operation/6ef6b6f1-30c4-4115-b98c-1ca323b50ac0")
@vh3.decorator.nirvana_names(yt_token="yt-token", base_path="base_path")
@vh3.decorator.nirvana_output_names("outTable")
@vh3.decorator.nirvana_names_transformer(vh3.name_transformers.snake_to_camel)
def get_mr_table(
    *,
    cluster: vh3.Enum[
        typing.Literal[
            "hahn",
            "banach",
            "freud",
            "marx",
            "hume",
            "arnold",
            "markov",
            "bohr",
            "landau",
            "seneca-vla",
            "seneca-sas",
            "seneca-man",
            "zeno",
        ]
    ] = None,
    creation_mode: vh3.Enum[typing.Literal["NO_CHECK",
                                           "CHECK_EXISTS"]] = "NO_CHECK",
    table: vh3.String = None,
    yt_token: vh3.Secret = None,
    file_with_table_name: vh3.Text = None,
    base_path: typing.Union[vh3.MRDirectory, vh3.MRFile, vh3.MRTable] = None
) -> vh3.MRTable:
    """
    Get MR Table

    Creates a reference to MR Table, either existing or potential.
      * If input `fileWithTableName` is present, its first line will be used as the table's path. If not, `table` option value will be used instead.
      * If `base_path` input is present, table path will be treated as *relative* and resolved against `base_path`. If not, path will be treated as *absolute*.

    :param cluster: Cluster:
      [[MR Cluster this table is on]]
      MR Cluster name, recognized by MR processor and FML processor.
      * If not set, `base_path`'s cluster will be used
      * If both `cluster` option value and `base_path` input are present, cluster name specified in **option** will be used
    :param creation_mode: Creation Mode:
      [[Actions to take when getting the MR Table]]
      MR Path creation mode. Specifies additional actions to be taken when getting the path
    :param table: Table:
      [[Path to MR Table]]
      Path to MR table. Used when `fileWithTableName` input is absent.
      * If `base_path` input is absent, this is an absolute path.
      * If `base_path` input is present, this is a relative path.
    :param yt_token: YT Token:
      [[(Optional) Token used if Creation Mode is "Check that Path Exists".
    Write the name of Nirvana Secret holding your YT Access Token here.]]
      *(Optional)* YT OAuth Token to use in "Check that Path Exists" Creation Mode. If not specified, MR Processor's token will be used.

      [Obtain access token](https://nda.ya.ru/3RSzVU), then [create a Nirvana secret](https://nda.ya.ru/3RSzWZ) and [use it here](https://nda.ya.ru/3RSzWb).
      You can [share the secret](https://nda.ya.ru/3RSzWd) with user(s) and/or a staff group.
    :param file_with_table_name:
      Text file with MR table path on its first line. If this input is absent, `table` option value will be used instead.
      * If `base_path` input is absent, this is an absolute path.
      * If `base_path` input is present, this is a relative path.
    :param base_path:
      Base path to resolve against.

      If absent, table path is considered absolute.
    """
    raise NotImplementedError("Write your local execution stub here")


@vh3.decorator.external_operation("https://nirvana.yandex-team.ru/operation/84de6480-2091-47e4-b6f7-3aa5bfbae472")
@vh3.decorator.nirvana_names(bundle_tar_gz="bundle.tar.gz")
@vh3.decorator.nirvana_output_names("dst")
@vh3.decorator.nirvana_names_transformer(vh3.name_transformers.snake_to_dash, options=True)
def antifraud_runner(
    *,
    yt_token: vh3.Secret,
    mr_account: vh3.String,
    bundle_tar_gz: vh3.Binary,
    src: vh3.MRTable,
    job_layer: vh3.MultipleStrings = (
        "01d15282-fecf-4c29-8771-1daeb410eeee", "c0996254-74bf-4d43-a0f6-d1e453f70c19"),
    max_ram: vh3.Integer = 512,
    mr_default_cluster: vh3.Enum[
        typing.Literal[
            "hahn",
            "freud",
            "marx",
            "hume",
            "arnold",
            "markov",
            "bohr",
            "landau",
            "seneca-vla",
            "seneca-sas",
            "seneca-man",
        ]
    ] = "arnold"
) -> vh3.MRTable:
    """
    antifraud runner

    :param bundle_tar_gz:
    :param job_layer:
    :param mr_account: MR Account:
      [[MR Account Name.
    By default, output tables and directories will be created in some subdirectory of home/<MR Account>/<workflow owner>/nirvana]]
      MR account name (e.g. `rank_machine`) used to build MR output path for this operation.

      See the `mr-output-path` option for more information
    :param mr_default_cluster: Default YT cluster:
      [[Default YT cluster]]
    """
    raise NotImplementedError("Write your local execution stub here")


class Yql1Output(typing.NamedTuple):
    output1: vh3.OptionalOutput[vh3.MRTable]
    directory: vh3.OptionalOutput[vh3.MRDirectory]


@vh3.decorator.external_operation("https://nirvana.yandex-team.ru/operation/6356092e-a511-49d2-9dc5-7ea81adcde9c")
@vh3.decorator.nirvana_names(
    py_code="py_code",
    py_export="py_export",
    py_version="py_version",
    use_account_tmp="use_account_tmp",
    code_revision="code_revision",
    code_work_dir="code_work_dir",
    arcanum_token="arcanum_token",
    svn_user_name="svn_user_name",
    svn_user_id_rsa="svn_user_id_rsa",
    svn_operation_source="svn_operation_source",
    yql_server="yql_server",
)
@vh3.decorator.nirvana_names_transformer(vh3.name_transformers.snake_to_dash, options=True)
def yql_1(
    *,
    mr_account: vh3.String,
    yt_token: vh3.Secret,
    yql_token: vh3.Secret,
    request: vh3.String = "INSERT INTO {{output1}} SELECT * FROM {{input1}};",
    py_code: vh3.String = None,
    py_export: vh3.MultipleStrings = (),
    py_version: vh3.Enum[typing.Literal["Python2",
                                        "ArcPython2", "Python3"]] = "Python3",
    mr_default_cluster: vh3.Enum[
        typing.Literal[
            "hahn",
            "freud",
            "marx",
            "hume",
            "arnold",
            "markov",
            "bohr",
            "landau",
            "seneca-vla",
            "seneca-sas",
            "seneca-man",
        ]
    ] = "hahn",
    yt_pool: vh3.String = None,
    ttl: vh3.Integer = 7200,
    max_ram: vh3.Integer = 256,
    max_disk: vh3.Integer = 1024,
    timestamp: vh3.String = None,
    param: vh3.MultipleStrings = (),
    mr_output_path: vh3.String = None,
    yt_owners: vh3.String = None,
    use_account_tmp: vh3.Boolean = False,
    code_revision: vh3.String = None,
    code_work_dir: vh3.String = None,
    arcanum_token: vh3.Secret = None,
    svn_user_name: vh3.String = None,
    svn_user_id_rsa: vh3.Secret = None,
    svn_operation_source: vh3.MultipleStrings = (),
    yql_operation_title: vh3.String = "YQL Nirvana Operation: {{nirvana_operation_url}}",
    yql_server: vh3.String = "yql.yandex.net",
    mr_output_ttl: vh3.Integer = None,
    retries_on_job_failure: vh3.Integer = 0,
    retries_on_system_failure: vh3.Integer = 10,
    job_metric_tag: vh3.String = None,
    mr_transaction_policy: vh3.Enum[typing.Literal["MANUAL", "AUTO"]] = "AUTO",
    input1: typing.Sequence[vh3.MRTable] = (),
    files: typing.Sequence[
        typing.Union[
            vh3.Binary,
            vh3.Executable,
            vh3.HTML,
            vh3.Image,
            vh3.JSON,
            vh3.MRDirectory,
            vh3.MRTable,
            vh3.TSV,
            vh3.Text,
            vh3.XML,
        ]
    ] = ()
) -> Yql1Output:
    """
    YQL 1

    Apply YQL script on MapReduce

    Code: https://a.yandex-team.ru/arc/trunk/arcadia/dj/nirvana/operations/yql/yql

    User guide: https://wiki.yandex-team.ru/nirvana-ml/ml-marines/#yql

    :param mr_account: MR Account:
      [[MR Account Name.
    By default, output tables and directories will be created in some subdirectory of home/<MR Account>/<workflow owner>/nirvana]]
      MR account name (e.g. `rank_machine`) used to build MR output path for this operation.

      See the `mr-output-path` option for more information
    :param yt_token: YT Token:
      [[ID of Nirvana Secret with YT access token (https://nda.ya.ru/3RSzVU).
    Guide to Nirvana Secrets: https://nda.ya.ru/3RSzWZ]]
      YT OAuth Token.

        [Obtain access token](https://nda.ya.ru/3RSzVU), then [create a Nirvana secret](https://nda.ya.ru/3RSzWZ) and [use it here](https://nda.ya.ru/3RSzWb).
        You can [share the secret](https://nda.ya.ru/3RSzWd) with user(s) and/or a staff group.
    :param yql_token: YQL Token:
      [[YQL OAuth Token, see https://wiki.yandex-team.ru/kikimr/yql/userguide/cli/#autentifikacija]]
      YQL OAuth Token, see https://wiki.yandex-team.ru/kikimr/yql/userguide/cli/#autentifikacija
    :param request: Request
      [[YQL request]]
      YQL request
    :param py_code: Python Code
      [[Python user defined functions definition]]
      Python user defined functions definition
    :param py_export: Python Export
      [[Python user defined functions declaration]]
      Python user defined functions declaration
    :param py_version: Python Version
      [[Python user defined functions version, https://clubs.at.yandex-team.ru/yql/2400]]
      Python user defined functions version, https://clubs.at.yandex-team.ru/yql/2400
    :param mr_default_cluster: Default YT cluster:
      [[Default YT cluster]]
      Default YT cluster
    :param yt_pool: YT Pool:
      [[Pool used by YT scheduler. Leave blank to use default pool.
    This option has no effect on YaMR.]]
      Pool used by [YT operation scheduler](https://nda.ya.ru/3Rk4af). Leave this blank to use default pool.
    :param timestamp: Timestamp for caching
      [[Any string used for Nirvana caching only]]
      Any string used for Nirvana caching only
    :param param: Parameters
      [[List of 'name=value' items which could be accessed as {{param[name]}}]]
      List of 'name=value' items which could be accessed as {{param[name]}}
    :param mr_output_path: MR Output Path:
      [[Directory for output MR tables and directories.
    Limited templating is supported: `${param["..."]}`, `${meta["..."]}`, `${mr_input["..."]}` (path to input MR *directory*) and `${uniq}` (= unique path-friendly string).]]
      Directory for output MR tables and directories.

      Limited templating is supported: `${param["..."]}`, `${meta["..."]}`, `${mr_input["..."]}` (path to input MR *directory*) and `${uniq}` (= unique path-friendly string).

      The default template for `mr-output-path` is

              home[#if param["mr-account"] != meta.owner]/${param["mr-account"]}[/#if]/${meta.owner}/nirvana/${meta.operation_uid}

      If output path does not exist, it will be created.

      Temporary directory, `${mr_tmp}`, is derived from output path in an unspecified way. It is ensured that:
        * It will exist before `job-command` is started
        * It need not be removed manually after execution ends. However, you **should** remove all temporary data created in `${mr_tmp}`, even if your command fails
    :param yt_owners: YT Owners
      [[Additional YT users allowed to read and manage operations]]
      Additional YT users allowed to read and manage operations
    :param use_account_tmp: Use tmp in account
      [[Use tmp folder in account but not in //tmp for avoid fails due to tmp overquota, recommended for production processes]]
      Use tmp folder in account but not in //tmp for avoid fails due to tmp overquota, recommended for production processes
    :param code_revision: Code default revision
      [[Default code revision for {{arcadia:/...}}]]
      Default code revision for {{arcadia:/...}}
    :param code_work_dir: Code default directory
      [[Default code working directory for {{./...}}]]
      Default code working directory for {{./...}}
    :param arcanum_token: Arcanum Token
      [[Arcanum Token, see https://wiki.yandex-team.ru/arcanum/api/]]
      Arcanum Token, see https://wiki.yandex-team.ru/arcanum/api/
    :param svn_user_name: SVN User name
      [[SVN user name for operation source and {{arcadia:/...}}]]
      SVN user name for operation source and {{arcadia:/...}}
    :param svn_user_id_rsa: SVN User private key
      [[SVN user private key for operation source and {{arcadia:/...}}]]
      SVN user private key for operation source and {{arcadia:/...}}
    :param svn_operation_source: SVN Operation source
      [[The YQL operation source path on SVN, should start with arcadia:/ or svn+ssh://, may contain @revision]]
      The YQL operation source path on SVN, should start with arcadia:/ or svn+ssh://, may contain @revision
    :param yql_operation_title: YQL Operation title
      [[YQL operation title for monitoring]]
      YQL operation title for monitoring
    :param yql_server: YQL server
      [[YQL server]]
      YQL server (default: yql.yandex.net)
    :param mr_output_ttl: MR Output TTL, days:
      [[TTL in days for mr-output-path directory and outputs which are inside the directory]]
      TTL in days for mr-output-path directory and outputs which are inside the directory
    :param job_metric_tag: Job metric tag
      [[Tag for monitoring of resource usage]]
      Tag for monitoring of resource usage
    :param mr_transaction_policy: MR Transaction policy
      [[Transaction policy, in auto policy yql operations are canceled when nirvana workflow in canceled]]
      Transaction policy, in auto policy yql operations are canceled when nirvana workflow in canceled
    :param input1:
      Input 1
    :param files:
      Attached files: if link_name is specified it is interpreted as file name, otherwise the input is unpacked as tar archive
    """
    raise NotImplementedError("Write your local execution stub here")


@vh3.decorator.external_operation("https://nirvana.yandex-team.ru/operation/7b6e03f0-c942-4911-b276-bc81d705c59b")
@vh3.decorator.nirvana_names(timestamp="Timestamp", sync="Sync")
@vh3.decorator.nirvana_output_names("YA_PACKAGE")
def ya_package(
    *,
    packages: vh3.String = None,
    package_type: vh3.Enum[typing.Literal["tarball", "debian"]] = "tarball",
    use_new_format: vh3.Boolean = False,
    strip_binaries: vh3.Boolean = False,
    resource_type: vh3.String = "YA_PACKAGE",
    arcadia_patch: vh3.String = None,
    use_aapi_fuse: vh3.Boolean = True,
    use_arc_instead_of_aapi: vh3.Boolean = False,
    aapi_fallback: vh3.Boolean = False,
    run_tests: vh3.Boolean = False,
    checkout: vh3.Boolean = False,
    use_ya_dev: vh3.Boolean = False,
    sandbox_oauth_token: vh3.Secret = None,
    arcadia_url: vh3.String = "arcadia:/arc/trunk/arcadia",
    arcadia_revision: vh3.Integer = None,
    checkout_arcadia_from_url: vh3.String = None,
    kill_timeout: vh3.Integer = None,
    sandbox_requirements_disk: vh3.Integer = None,
    sandbox_requirements_ram: vh3.Integer = None,
    cache: vh3.String = None,
    build_type: vh3.Enum[
        typing.Literal["release", "debug", "profile", "coverage",
                       "relwithdebinfo", "valgrind", "valgrind-release"]
    ] = "release",
    host_platform: vh3.String = None,
    target_platform: vh3.String = None,
    clear_build: vh3.Boolean = False,
    sanitize: vh3.Enum[typing.Literal["undefined",
                                      "address", "memory", "thread"]] = None,
    compress_package_archive: vh3.Boolean = True,
    owner: vh3.String = None,
    timestamp: vh3.Date = None,
    build_system: vh3.Enum[typing.Literal["ya",
                                          "ya_force", "semi_distbuild", "distbuild"]] = "ya",
    ya_yt_token_vault_owner: vh3.String = None,
    ya_yt_token_vault_name: vh3.String = None,
    sync: typing.Union[
        vh3.Binary,
        vh3.Executable,
        vh3.FMLDumpParse,
        vh3.FMLFormula,
        vh3.FMLFormulaSerpPrefs,
        vh3.FMLPool,
        vh3.FMLPrs,
        vh3.FMLSerpComparison,
        vh3.FMLWizards,
        vh3.File,
        vh3.HTML,
        vh3.HiveTable,
        vh3.Image,
        vh3.JSON,
        vh3.MRDirectory,
        vh3.MRFile,
        vh3.MRTable,
        vh3.TSV,
        vh3.Text,
        vh3.XML,
    ] = None
) -> vh3.Binary:
    """
    YA_PACKAGE

    **Назначение операции**

    Создает архив из файлов Arcadia c помощью Sandbox-задачи YA_PACKAGE.

    **Описание входов**

    - "sync" - любые данные, используются для синхронизации выполнения с другими операциями.

    **Описание выходов**

    - "YA_PACKAGE" - архив (бинарный файл).


    **Ограничения**

    Не предусмотрены.

    :param timestamp:
    :param sync:
    :param packages: Package paths relative to arcadia, `;` separated
    :param package_type: Package type: debian or tarball
    :param use_new_format: New ya package json format
      [[ya package --new]]
    :param strip_binaries: Strip debug information
    :param resource_type: Created resource type
    :param arcadia_patch: Apply patch (diff file rbtorrent, paste.y-t.ru link or plain text).
    :param use_aapi_fuse: Use arcadia-api fuse
    :param use_arc_instead_of_aapi: Use arc fuse instead of aapi
    :param aapi_fallback: Fallback to svn/hg if AAPI services are temporary unavailable
    :param run_tests: Run tests after build
    :param checkout: Run ya make with --checkout
    :param use_ya_dev: Use ya-dev to build
    :param sandbox_oauth_token: OAuth token secret
    :param arcadia_url: Arcadia base URL
    :param arcadia_revision: Arcadia revision
    :param checkout_arcadia_from_url: Full SVN url for arcadia (Overwrites base URL and revision, use @revision to fix revision)
    :param kill_timeout: Kill Timeout (seconds)
    :param sandbox_requirements_disk: Disk requirements in Mb
    :param sandbox_requirements_ram: RAM requirements in Mb
    :param cache: Force cache invalidation
    :param build_type: Build type
    :param host_platform: Host platform
    :param target_platform: Target platform
    :param clear_build: Clear build
    :param sanitize: Build with sanitizer
    :param compress_package_archive: Compress package archive
    :param owner:
      Owner of sandbox task
    :param build_system: Build system
    :param ya_yt_token_vault_owner: YT token vault owner
    :param ya_yt_token_vault_name: YT token vault name
    """
    raise NotImplementedError("Write your local execution stub here")


class BuildArcadiaProjectOutput(typing.NamedTuple):
    arcadia_project: vh3.Executable
    sandbox_task_id: vh3.Text


@vh3.decorator.external_operation("https://nirvana.yandex-team.ru/operation/dd4b5735-1ee7-497d-91fc-b81ba8b510fc")
@vh3.decorator.nirvana_output_names(arcadia_project="ARCADIA_PROJECT", sandbox_task_id="SANDBOX_TASK_ID")
def build_arcadia_project(
    *,
    targets: vh3.String,
    arts: vh3.String,
    arcadia_url: vh3.String = "arcadia:/arc/trunk/arcadia",
    arcadia_revision: vh3.Integer = None,
    checkout_arcadia_from_url: vh3.String = None,
    build_type: vh3.Enum[
        typing.Literal["release", "debug", "profile", "coverage",
                       "relwithdebinfo", "valgrind", "valgrind-release"]
    ] = "release",
    arts_source: vh3.String = None,
    result_single_file: vh3.Boolean = False,
    definition_flags: vh3.String = None,
    sandbox_oauth_token: vh3.Secret = None,
    arcadia_patch: vh3.String = None,
    owner: vh3.String = None,
    use_aapi_fuse: vh3.Boolean = True,
    use_arc_instead_of_aapi: vh3.Boolean = True,
    aapi_fallback: vh3.Boolean = False,
    kill_timeout: vh3.Integer = None,
    sandbox_requirements_disk: vh3.Integer = None,
    sandbox_requirements_ram: vh3.Integer = None,
    sandbox_requirements_platform: vh3.Enum[
        typing.Literal[
            "Any",
            "darwin-20.4.0-x86_64-i386-64bit",
            "linux",
            "linux_ubuntu_10.04_lucid",
            "linux_ubuntu_12.04_precise",
            "linux_ubuntu_14.04_trusty",
            "linux_ubuntu_16.04_xenial",
            "linux_ubuntu_18.04_bionic",
            "osx",
            "osx_10.12_sierra",
            "osx_10.13_high_sierra",
            "osx_10.14_mojave",
            "osx_10.15_catalina",
            "osx_10.16_big_sur",
        ]
    ] = None,
    checkout: vh3.Boolean = False,
    clear_build: vh3.Boolean = True,
    strip_binaries: vh3.Boolean = False,
    lto: vh3.Boolean = False,
    thinlto: vh3.Boolean = False,
    musl: vh3.Boolean = False,
    use_system_python: vh3.Boolean = False,
    target_platform_flags: vh3.String = None,
    javac_options: vh3.String = None,
    ya_yt_proxy: vh3.String = None,
    ya_yt_dir: vh3.String = None,
    ya_yt_token_vault_owner: vh3.String = None,
    ya_yt_token_vault_name: vh3.String = None,
    result_rt: vh3.String = None,
    timestamp: vh3.Date = None,
    build_system: vh3.Enum[typing.Literal["ya",
                                          "ya_force", "semi_distbuild", "distbuild"]] = "ya"
) -> BuildArcadiaProjectOutput:
    """
    Build Arcadia Project

    Launches YA_MAKE task in Sandbox for provided target and downloads requested artifact.

    :param targets: Target
      [[Multiple targets with ";" are not allowed]]
    :param arts: Build artifact
      [[Multiple artifacts with ";" and custom destination directory with "=" are not allowed]]
    :param arcadia_url: Svn url for arcadia
      [[Should not contain revision]]
    :param arcadia_revision: Arcadia Revision
    :param checkout_arcadia_from_url: Full SVN url for arcadia (Overwrites base URL and revision, use @revision to fix revision)
    :param build_type: Build type
    :param arts_source: Source artifacts (semicolon separated pairs path[=destdir])
      [[Какие файлы из Аркадии поместить в отдельный ресурс (формат тот же, что и у build artifacts)]]
    :param result_single_file: Result is a single file
    :param definition_flags: Definition flags
      [[For example "-Dkey1=val1 ... -DkeyN=valN"]]
    :param sandbox_oauth_token: Sandbox OAuth token
      [[To run task on behalf of specific user]]
      Name of the secret containing oauth token of user the sandbox task should be launched from
    :param arcadia_patch: Apply patch
      [[Diff file rbtorrent, paste.y-t.ru link or plain text. Doc: https://nda.ya.ru/3QTTV4]]
    :param owner: Custom sandbox task owner (should be used only with OAuth token)
      [[OAuth token owner should be a member of sandbox group]]
    :param use_aapi_fuse: Use arcadia-api fuse
    :param use_arc_instead_of_aapi: Use arc fuse instead of aapi
    :param aapi_fallback: Fallback to svn/hg if AAPI services are temporary unavailable
    :param kill_timeout: Kill Timeout (seconds)
    :param sandbox_requirements_disk: Disk requirements in Mb
    :param sandbox_requirements_ram: RAM requirements in Mb
    :param sandbox_requirements_platform: Platform
    :param checkout: Run ya make with --checkout
    :param clear_build: Clear build
    :param strip_binaries: Strip result binaries
    :param lto: Build with LTO
    :param thinlto: Build with ThinLTO
    :param musl: Build with musl-libc
    :param use_system_python: Use system Python to build python libraries
    :param target_platform_flags: Target platform flags (only for cross-compilation)
    :param javac_options: Javac options (semicolon separated)
    :param ya_yt_proxy: YT store proxy
    :param ya_yt_dir: YT store cypress path
    :param ya_yt_token_vault_owner: YT token vault owner
    :param ya_yt_token_vault_name: YT token vault name
    :param result_rt: Result resource type
    :param timestamp: Timestamp
    :param build_system: Build System
    """
    raise NotImplementedError("Write your local execution stub here")


@vh3.decorator.external_operation("https://nirvana.yandex-team.ru/operation/f42eeefa-4f48-43cc-9472-47a9c8c7ced3")
@vh3.decorator.nirvana_names_transformer(vh3.name_transformers.snake_to_dash, options=True)
def antifraud_update_collapsed_aggrs(
    *,
    yt_token: vh3.Secret,
    batch_size: vh3.Integer,
    timeout_sec: vh3.Integer,
    host: vh3.String,
    retry_delay_sec: vh3.Number,
    retries: vh3.Integer,
    exe: vh3.Executable,
    src: vh3.MRTable,
    mr_default_cluster: vh3.Enum[
        typing.Literal[
            "hahn",
            "freud",
            "marx",
            "hume",
            "arnold",
            "markov",
            "bohr",
            "landau",
            "seneca-vla",
            "seneca-sas",
            "seneca-man",
        ]
    ] = "arnold"
) -> None:
    """
    Antifraud update collapsed aggrs

    :param yt_token: YT Token:
      [[ID of Nirvana Secret with YT access token (https://nda.ya.ru/3RSzVU).
    Guide to Nirvana Secrets: https://nda.ya.ru/3RSzWZ]]
      YT OAuth Token.

        [Obtain access token](https://nda.ya.ru/3RSzVU), then [create a Nirvana secret](https://nda.ya.ru/3RSzWZ) and [use it here](https://nda.ya.ru/3RSzWb).
        You can [share the secret](https://nda.ya.ru/3RSzWd) with user(s) and/or a staff group.
    :param mr_default_cluster: Default YT cluster:
      [[Default YT cluster]]
    """
    raise NotImplementedError("Write your local execution stub here")


YQL_JOIN = """PRAGMA yson.DisableStrict;
PRAGMA yson.AutoConvert;
PRAGMA yt.InferSchema = '1';
PRAGMA yt.Pool = "processing-mail-so";
PRAGMA AnsiInForEmptyOrNullableItemsCollections;

$source = (
    SELECT Yson::LookupString(nsrc, "txn_extid") as id,
    Yson::LookupString(nsrc, "transaction_type") as transaction_type,
    channel_uri,
    storage_service,
    nsrc,
    request,
    rbl,
    lua_resolution,
    queues
    FROM {{input1}}
);

$scores = (SELECT * FROM $source WHERE request = "score" AND transaction_type in ("BINDING", "COMMON_PAYMENT", "AUTH"));
$saves = (SELECT * FROM $source WHERE request = "save");

INSERT INTO {{output1}}
SELECT
    scores.id as id,
    scores.channel_uri as channel_uri,
    scores.storage_service as storage_service,
    scores.rbl as rbl,
    scores.nsrc as score_data,
    scores.lua_resolution as lua_resolution,
    scores.queues as queues,
    saves.nsrc as save_data
FROM       $scores AS scores
LEFT  JOIN $saves AS saves
USING(id);"""

MR_ACCOUNT = "so_fml"
YT_TOKEN = "luckybug_nirvana_token"
YQL_TOKEN = "luckybug_yql_token"


@vh3.decorator.graph()
def collapse_daily_aggregates() -> None:
    src = get_mr_table(
        table="//logs/mail-so-antifraud-log/1d/2022-03-30", cluster="arnold")

    joined = yql_1(input1=[src], request=YQL_JOIN, yt_token=YT_TOKEN,
                   yql_token=YQL_TOKEN, mr_account=MR_ACCOUNT)

    runner = ya_package(
        packages="mail/so/daemons/antifraud/antifraud_runner/package.json", arcadia_revision=9301950)

    updater = build_arcadia_project(targets="mail/so/daemons/antifraud/nirvana/update_collapsed_aggrs",
                                    arts="mail/so/daemons/antifraud/nirvana/update_collapsed_aggrs/update_collapsed_aggrs",
                                    arcadia_revision=9301950)

    aggrs = antifraud_runner(
        bundle_tar_gz=runner, src=joined.output1, yt_token=YT_TOKEN, mr_account=MR_ACCOUNT)

    return antifraud_update_collapsed_aggrs(exe=updater.arcadia_project,
                                            src=aggrs,
                                            yt_token=YT_TOKEN,
                                            batch_size=1000,
                                            timeout_sec=10,
                                            retries=10,
                                            retry_delay_sec=0.5,
                                            host="http://so-fraud-producer.pers.yandex.net")
