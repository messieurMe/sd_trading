import Constants.host1
import io.ktor.application.*
import io.ktor.http.*
import io.ktor.response.*
import io.ktor.routing.*
import io.ktor.server.engine.*
import io.ktor.server.jetty.*
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.util.concurrent.ConcurrentHashMap

private val serverMap = ConcurrentHashMap<String, PaperInfo>()

fun main() {
    val server = Server()
    server()
}

class Server {

    private val ok: suspend ApplicationCall.(String) -> Unit
        get() = { r -> this.respondText(r, status = HttpStatusCode.OK) }

    private val error: suspend ApplicationCall.(String) -> Unit
        get() = { r -> this.respondText(r, status = HttpStatusCode.BadRequest) }

    operator fun invoke() {
        embeddedServer(Jetty, host = host1, port = 8080) {
            routing {
                get("/add") {
                    runCatching {
                        val price = call.parameters["price"]!!.toInt()
                        val amount = call.parameters["amount"]!!.toInt()
                        val name = call.parameters["name"]!!

                        serverMap[name] = (serverMap[name] ?: PaperInfo(0, 0, name)).let { paper ->
                            paper.copy(
                                paper.price + price,
                                paper.amount + amount
                            )
                        }
                    }
                        .onSuccess { call.ok(it.toString()) }
                        .onFailure { call.error(it.message!!) }
                }

                get("/info") { Json.encodeToString(serverMap["stock"]) }


                get("/sell") {
                    kotlin.runCatching {
                        val stock = call.parameters["stock"]!!
                        val diff = call.parameters["amount"]!!.toIntOrNull()!!
                        val buyPrice = call.parameters["price"]!!

                        val x = serverMap[stock]!!
                        serverMap[stock] = x.copy(x.amount - diff)
                    }.onFailure { e -> call.error(e.message ?: "") }
                }

                get("/change") {
                    runCatching {
                        val stock = call.parameters["stock"]!!
                        val newPrice = call.parameters["price"]!!.toInt()

                        val x = serverMap[stock]!!
                        serverMap[stock] = x.copy(newPrice)
                        "Ok"
                    }.onSuccess { call.ok(it) }.onFailure { call.error(it.message ?: "") }
                }
            }
        }.start(wait = true)
    }
}