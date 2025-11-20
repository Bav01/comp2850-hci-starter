import io.ktor.server.application.*
import io.ktor.server.routing.*
import routes.taskRoutes

fun Application.configureRouting() {
    routing {
        taskRoutes()
    }
}
