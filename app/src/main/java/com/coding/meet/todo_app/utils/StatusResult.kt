package com.coding.meet.todo_app.utils

// (Ganti 'enum' menjadi 'sealed class' jika 'when' Anda sebelumnya error 'is over enum')
sealed class StatusResult {
    object Added : StatusResult()
    object Updated : StatusResult()
    object Deleted : StatusResult()
}