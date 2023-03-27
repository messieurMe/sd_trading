import io.ktor.application.*
import io.ktor.http.*
import io.ktor.response.*

    val error: suspend ApplicationCall.(String) -> Unit
        get() = { r -> this.respondText(r, status = HttpStatusCode.BadRequest) }

    val ok: suspend ApplicationCall.(String) -> Unit
        get() = { r -> this.respondText(r, status = HttpStatusCode.OK) }
