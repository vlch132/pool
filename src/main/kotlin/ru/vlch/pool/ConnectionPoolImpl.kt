package ru.vlch.pool

import java.sql.Connection
import java.sql.DriverManager
import java.sql.ResultSet
import java.util.concurrent.CompletableFuture
import java.util.concurrent.LinkedBlockingQueue
import java.util.concurrent.TimeUnit

/**
 * @param size Размер пула подключений.
 * @param jdbcUrl JDBC URL для подключения к базе данных.
 * @param waitConnection Максимальное время ожидания подключения (в секундах, по умолчанию 10 секунд).
 */
class ConnectionPoolImpl(
    private val size: Int,
    private val jdbcUrl: String,
    private val waitConnection: Long = 10,
) : ConnectionPool {

    private val connections = createConnections()

    override fun executeRow(query: String): List<Row> {
        // Запрос не может быть пустым
        if (query.isBlank()) throw RuntimeException("Query cannot be blank")

        // Забираем пул из connections, если в течении waitConnection секунд получить его не выйдет - выбрасываем ошибку
        val connection = connections.poll(waitConnection, TimeUnit.SECONDS) ?: throw RuntimeException("Failed get connection with $waitConnection")
        try {
            // Отправляем запрос в бд
            val resultSet = connection.createStatement().executeQuery(query)
            return resultSet.toList()
        } catch (e: Exception) {
            throw RuntimeException("Failed create connection with $query", e)
        } finally {
            // В обоих случяах надо вернуть пул обратно
            connections.put(connection)
            println("Connection $connection was closed. Thread ${Thread.currentThread().name} returned the connection to the pool.")
        }
    }

    // Не уверен, что так круто, но показалось, мы можем просто вызывать основной execute, но асинхронно
    override fun executeCompletableFuture(query: String): CompletableFuture<List<Row>> = CompletableFuture.supplyAsync { executeRow(query) }

    // Создаем подключения, но не больше чем size
    private fun createConnections(): LinkedBlockingQueue<Connection> {
        val result = LinkedBlockingQueue<Connection>()

        for (index in 0 until size) {
            result.add(DriverManager.getConnection(jdbcUrl))
        }

        return result
    }

    private fun ResultSet.toList(): List<Row> {
        val columnCount = metaData.columnCount
        val rows = mutableListOf<Row>()

        while (next()) {
            val values = (1..columnCount).map { getObject(it) }
            rows.add(Row(values))
        }

        return rows
    }
}