import io.ktor.client.*
import io.ktor.client.engine.apache.*
import io.ktor.client.statement.*
import kotlinx.coroutines.runBlocking
import org.junit.Before
import org.junit.ClassRule
import org.junit.Test
import org.testcontainers.containers.FixedHostPortGenericContainer
import io.ktor.client.request.get
import io.ktor.http.*
import org.example.Client

class ClientTest {
    companion object {
        @ClassRule
        @JvmStatic
        fun server() = FixedHostPortGenericContainer("sd_trading:1.0")
            .withFixedExposedPort(8081, 8080)
            .withExposedPorts(8080)
    }

    private lateinit var testClient: HttpClient

    @Before
    fun init() {
        testClient = HttpClient(Apache) { expectSuccess = false }
    }

    private val path = ""

    private fun HttpResponse.isOk() = this.status == HttpStatusCode.OK
    private fun HttpResponse.isNotOk() = this.status == HttpStatusCode.BadRequest

    private suspend fun get(args: String) = testClient.get<HttpResponse>("$path/$args")

    @Test
    fun test(): Unit = runBlocking {
        val id = 69;
        Client().also { it(testClient) }

        assert(get("newUser?id=$id").isOk())
        assert(get("addMoney?id=$id&amount=100").isOk())
        assert(get("addMoney?id=$id").isNotOk())
        assert(get("addMoney").isNotOk())
        assert(get("addMoney?amount=100").isNotOk())

        assert(get("newCompany?name=A&price=1&amount=1").isOk())
        assert(get("buy?id=$id&paper=A&amount=10").isNotOk())
        assert(get("buy?id=A$id&paper=A&amount=10").isNotOk())
        assert(get("buy?id=$id&paper=B&amount=10").isNotOk())
        assert(get("buy?id=$id&paper=A&amount=1").isOk())

        assert(get("sell?id=$id&paper=A&amount=10").isNotOk())
        assert(get("sell?paper=A&amount=10").isNotOk())
        assert(get("sell?id=$id&amount=10").isNotOk())
        assert(get("sell?id=$id&paper=A&").isNotOk())
        assert(get("sell?id=$id&paper=B&amount=10").isNotOk())
        assert(get("sell?id=A$id&paper=A&amount=10").isNotOk())
        assert(get("sell?id=$id&paper=A&amount=1").isOk())

    }
}