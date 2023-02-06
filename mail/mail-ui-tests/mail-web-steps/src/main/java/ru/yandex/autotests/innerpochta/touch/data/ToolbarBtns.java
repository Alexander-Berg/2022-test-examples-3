package ru.yandex.autotests.innerpochta.touch.data;

/**
 * @author cosmopanda
 */
public enum ToolbarBtns {

    REPLY("Ответить"),
    REPLY_ALL("Ответить всем"),
    FORWARD("Переслать"),
    INSPAM("Это спам!"),
    INARCHIVE("В архив"),
    INFOLDER("В папку"),
    MARKUNREAD("Не прочитано"),
    MARKLABEL("Метки"),
    PIN("Закрепить"),
    DELETE("Удалить"),
    WRITE("Дописать"),
    UNPIN("Открепить"),
    CREATEVENT("Создать событие"),
    MSGHEADERS("Свойства письма");

    private String btn;

    ToolbarBtns(String btn) {
        this.btn = btn;
    }

    public String btn() {
        return btn;
    }
}
