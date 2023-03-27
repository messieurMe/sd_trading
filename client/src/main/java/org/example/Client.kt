package org.example

import Constants.host1
import Constants.url
import PaperInfo
import UserInfo
import io.ktor.application.*
import io.ktor.client.*
import io.ktor.client.engine.apache.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*
import io.ktor.response.*
import io.ktor.routing.*
import io.ktor.server.engine.*
import io.ktor.server.jetty.*
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.lang.IllegalArgumentException
import kotlin.collections.component1
import kotlin.collections.component2
import kotlin.collections.getValue
import kotlin.collections.map
import kotlin.collections.mutableMapOf
import kotlin.collections.set
import kotlin.collections.sum


fun main(args: Array<String>) {
    val clients = Client()

    HttpClient(Apache) { expectSuccess = false }.also {
        clients(it, shouldWait = true)
    }
}

class Client {
    private val clients = HashMap<Int, UserInfo>()

    private val ok: suspend ApplicationCall.(String) -> Unit
        get() = { r -> this.respondText(r, status = HttpStatusCode.OK) }

    private val error: suspend ApplicationCall.(String) -> Unit
        get() = { r -> this.respondText(r, status = HttpStatusCode.BadRequest) }

    private fun ApplicationCall.id(): Int = parameters["id"]?.toIntOrNull() ?: throw IllegalArgumentException("No id")

    suspend fun HttpClient.callAbout(stock: String): PaperInfo {
        val response = get("$url/info?paper=$stock") as HttpResponse
        return Json.decodeFromString(response.readText())
    }


    operator fun invoke(exchange: HttpClient, shouldWait: Boolean = false) {
        embeddedServer(Jetty, host = host1, port = 8081) {
            routing {
                get("/newUser") {
                    runCatching {
                        with(call.id()) {
                            clients.computeIfAbsent(this) { UserInfo(this, mutableMapOf(), 0) }
                        }
                    }.onSuccess { r -> call.ok(r.toString()) }.onFailure { e -> call.error(e.toString()); }
                }

                get("/newCompany") {
                    kotlin.runCatching {
                        with(call.id()) {
                            exchange.get<HttpResponse>(
                                "$url/add?" + "name=${call.parameters["name"]}&" + "amount=${call.parameters["amount"]}&" + "price=${call.parameters["price"]}"
                            )
                        }
                    }.onSuccess { call.ok(it.toString()) }.onFailure { call.error(it.message ?: "Incorrect arguments") }
                }

                get("/addMoney") {

                    runCatching {
                        clients.computeIfPresent(call.id()) { _, value ->
                            value.copy(
                                balance = value.balance + call.parameters["amount"]!!.toIntOrNull()!!
                            )
                        }
                    }.onSuccess { r -> call.ok(r.toString()) }.onFailure { e -> call.error("Incorrect argument") }
                }

                get("/sum") {
                    runCatching {
                        clients.getValue(call.id()).papers.map { (stock, amount) -> exchange.callAbout(stock).price * amount }
                            .sum()
                    }.onSuccess { r -> call.ok(r.toString()) }.onFailure { e -> call.error(e.toString()) }
                }

                get("all") {
                    runCatching {
                        Json.encodeToString(clients.getValue(call.id()).papers as Map<String, Int>)
                    }.onSuccess { r -> call.ok(r) }.onFailure { e -> call.error(e.toString()) }
                }

                get("/buy") {
                    runCatching {

                        val requestAmount = call.parameters["amount"]?.toIntOrNull() ?: notEnoughInformation()
                        val paper = call.parameters["paper"] ?: notEnoughInformation()

                        with(clients[call.id()]!!) {
                            papers[paper] = (papers[paper] ?: 0) + requestAmount
                            balance -= requestAmount
                        }
                    }.onSuccess { r -> call.ok(r.toString()) }.onFailure { e -> call.error(e.toString()) }
                }

                get("sell") {
                    runCatching {
                        val requestAmount = call.parameters["amount"]?.toIntOrNull() ?: notEnoughInformation()
                        val paper = call.parameters["paper"] ?: notEnoughInformation()

                        with(clients[call.id()]!!) {
                            papers[paper] = papers[paper]!! - requestAmount
                            balance += requestAmount
                        }

                    }.onSuccess { r -> call.ok(r.toString()) }.onFailure { e -> call.error(e.toString()) }

                }
            }
        }.start(wait = shouldWait)
    }

    private fun notEnoughInformation(): Nothing = throw IllegalArgumentException("Not enough info")

}