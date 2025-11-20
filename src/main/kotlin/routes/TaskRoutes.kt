package routes

import data.TaskRepository
import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import io.pebbletemplates.pebble.PebbleEngine
import java.io.StringWriter
// If you add the ktor htmx plugin, use this import:
// import io.ktor.server.htmx.*

fun Route.taskRoutes() {

    val pebble = PebbleEngine.Builder().build()

    // GET /tasks
    get("/tasks") {
        val model = mapOf(
            "title" to "Tasks",
            "tasks" to TaskRepository.all()
        )
        val template = pebble.getTemplate("templates/tasks/index.peb")

        val writer = StringWriter()
        template.evaluate(writer, model)

        call.respondText(writer.toString(), ContentType.Text.Html)
    }

    // POST /tasks (create new task)
    post("/tasks") {
        val title = call.receiveParameters()["title"].orEmpty().trim()

        // Validation
        if (title.isBlank()) {
            if (call.isHtmx()) {
                val error = """
                    <div id="status" hx-swap-oob="true" role="alert" aria-live="assertive">
                        Title is required. Please enter at least one character.
                    </div>
                """.trimIndent()

                return@post call.respondText(error, ContentType.Text.Html, HttpStatusCode.BadRequest)
            } else {
                return@post call.respondRedirect("/tasks?error=required")
            }
        }

        val task = TaskRepository.add(title)

        // HTMX request → return only the new list item and status message
        if (call.isHtmx()) {
            val fragment = """
                <li id="task-${task.id}">
                    <span>${task.title}</span>
                    <form action="/tasks/${task.id}/delete"
                          method="post"
                          style="display: inline;"
                          hx-post="/tasks/${task.id}/delete"
                          hx-target="#task-${task.id}"
                          hx-swap="outerHTML">
                        <button type="submit" aria-label="Delete task: ${task.title}">Delete</button>
                    </form>
                </li>
            """.trimIndent()

            val status = """
                <div id="status" hx-swap-oob="true">
                    Task "${task.title}" added successfully.
                </div>
            """.trimIndent()

            return@post call.respondText(fragment + status, ContentType.Text.Html, HttpStatusCode.Created)
        }

        // No-JS fallback
        call.respondRedirect("/tasks")
    }

    // POST /tasks/{id}/delete
    post("/tasks/{id}/delete") {
        val id = call.parameters["id"]?.toIntOrNull()
        val removed = id?.let { TaskRepository.delete(it) } ?: false

        if (call.isHtmx()) {
            val message = if (removed) "Task deleted." else "Could not delete task."

            val status = """
                <div id="status" hx-swap-oob="true">$message</div>
            """.trimIndent()

            // Replace the <li> with empty content (outerHTML swap)
            return@post call.respondText(status, ContentType.Text.Html)
        }

        call.respondRedirect("/tasks")
    }
}

// HTMX helper — remove this if you use io.ktor.server.htmx.*
fun ApplicationCall.isHtmx(): Boolean =
    request.headers["HX-Request"]?.equals("true", ignoreCase = true) == true
