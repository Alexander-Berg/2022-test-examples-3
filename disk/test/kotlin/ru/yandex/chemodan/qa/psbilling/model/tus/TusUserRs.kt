package ru.yandex.chemodan.qa.psbilling.model.tus

data class TusUserRs(
    val uid: String,
    val login: String,
    val password: String,
    val firstname: String,
    val lastname: String,
    val language: String,
    val country: String
)
