package ru.vlch.pool

import java.util.concurrent.CompletableFuture

interface ConnectionPool {
    fun executeRow(query: String): List<Row>
    fun executeCompletableFuture(query: String): CompletableFuture<List<Row>>
}