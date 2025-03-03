package ru.vlch.pool

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.api.assertDoesNotThrow
import org.junit.jupiter.api.assertThrows
import java.sql.DriverManager
import java.util.concurrent.CompletableFuture
import java.util.concurrent.TimeUnit

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class ConnectionPoolTest {
    private lateinit var pool: ConnectionPool

    @BeforeAll
    fun setup() {
        val jdbcUrl = "jdbc:h2:mem:test;MODE=PostgreSQL"

        pool = ConnectionPoolImpl(size = 2, jdbcUrl = jdbcUrl)

        // Заюзать мои же пулы не дает, просит делать так
        // Почему - понимаю, у меня executeQuery в методе вызывается потому что, а там select только возможен
        val connection = DriverManager.getConnection(jdbcUrl)
        connection.createStatement().execute("CREATE TABLE users (id INT PRIMARY KEY, name VARCHAR(255))")
        connection.createStatement().execute("INSERT INTO users VALUES (1, 'Sergey'), (2, 'Vladimir')")
        connection.close()
    }

    @Test
    fun `send query in executeRow should return correct rows`() {
        val result = pool.executeRow("SELECT * FROM users")

        assertEquals(2, result.size)
        assertEquals("Sergey", result[0].values[1])
        assertEquals("Vladimir", result[1].values[1])
    }

    @Test
    fun `send query in executeCompletableFuture should return correct rows`() {
        val future = pool.executeCompletableFuture("SELECT * FROM users")
        val result = future.get(2, TimeUnit.SECONDS)

        assertEquals(2, result.size)
        assertEquals("Sergey", result[0].values[1])
        assertEquals("Vladimir", result[1].values[1])
    }

    @Test
    fun `pool should limit connections`() {
        val future1 = pool.executeCompletableFuture("SELECT * FROM users")
        val future2 = pool.executeCompletableFuture("SELECT * FROM users")
        val future3 = pool.executeCompletableFuture("SELECT * FROM users")

        assertDoesNotThrow { CompletableFuture.allOf(future1, future2, future3).get(3, TimeUnit.SECONDS) }
    }

    @Test
    fun `execute should throw exception on invalid query`() {
        assertThrows<RuntimeException> {
            pool.executeRow("SELECT * FROM non_existing_table")
        }
    }

}