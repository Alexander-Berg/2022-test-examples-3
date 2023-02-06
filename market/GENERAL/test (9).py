from startrek_client import Startrek
import telebot

cnt = 11

bot = telebot.TeleBot('1945741779:AAE9QGKBZYP48kfLhOdRO24JsQZaOt6_yJQ')
bot.send_message(497141178, 'Офигеть! В очереди '+ str(cnt) +' открытых тикетов!')
