class BaseProcessDescription(object):
    """ Описание этапа интеграционного теста

    Attributes:
        desc                  Текстовое описание этапа.
        program               Имя бинарника приложения, используемого на system_cmd и post_cmd этапах.
        input_tables          Входные таблицы тестируемого приложения.
        output_tables         Выходные таблицы тестируемого приложения.
        binary                Имя бинарника тестируемого приложения. Если не указано, используется первое слово из desc.
        system_cmd            Команда, вызываемая перед запуском текущего этапа интеграционного теста.
        cmd                   Команда, вызов которой проверяется на этом этапе теста.
        post_cmd              Команда, вызываемая поле запуска текущего этапа интеграционного теста.
        suffix_dump_modes     Список пользовательских режимов приложения-дампера.
        regex_dump_modes      Список пользовательских режимов приложения-дампера.
        files_dir             Префикс пути к файлам-зависимостям тестируемого приложения.
    """

    desc = 'undescribed'
    program = None
    input_tables = []
    output_tables = []
    input_files = []
    output_files = []
    binary = ''
    system_cmd = ''
    cmd = ''
    post_cmd = ''
    suffix_dump_modes = []
    regex_dump_modes = []
    files_dir = '.'
    custom_fields = {}
