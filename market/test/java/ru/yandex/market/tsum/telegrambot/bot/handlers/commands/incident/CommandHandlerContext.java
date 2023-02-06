package ru.yandex.market.tsum.telegrambot.bot.handlers.commands.incident;

import org.telegram.telegrambots.meta.api.objects.Update;
import ru.yandex.market.tsum.clients.staff.StaffPerson;
import ru.yandex.market.tsum.telegrambot.bot.Bot;

public class CommandHandlerContext {
    Bot bot;
    Update update;
    StaffPerson person;

    public CommandHandlerContext() {
    }

    public CommandHandlerContext(Bot bot, Update update, StaffPerson person) {
        this.bot = bot;
        this.update = update;
        this.person = person;
    }

    public Bot getBot() {
        return bot;
    }

    public void setBot(Bot bot) {
        this.bot = bot;
    }

    public Update getUpdate() {
        return update;
    }

    public void setUpdate(Update update) {
        this.update = update;
    }

    public StaffPerson getPerson() {
        return person;
    }

    public void setPerson(StaffPerson person) {
        this.person = person;
    }
}
