package com.thebrownfoxx.neon.server.application.routing.authentication

import com.thebrownfoxx.neon.common.outcome.getOrElse
import com.thebrownfoxx.neon.server.application.dependency.DependencyProvider
import com.thebrownfoxx.neon.server.application.plugin.MemberIdClaim
import com.thebrownfoxx.neon.server.route.authentication.LoginBody
import com.thebrownfoxx.neon.server.route.authentication.LoginResponse
import com.thebrownfoxx.neon.server.route.authentication.LoginRoute
import com.thebrownfoxx.neon.server.service.authenticator.model.LoginError
import com.thebrownfoxx.neon.server.service.jwt.model.claimedAs
import io.ktor.http.HttpStatusCode
import io.ktor.server.request.receive
import io.ktor.server.resources.post
import io.ktor.server.response.respond
import io.ktor.server.routing.Route

fun Route.login() {
    with(DependencyProvider.dependencies) {
        post<LoginRoute> {
            val (username, password) = call.receive<LoginBody>()
            val memberId = authenticator.login(username, password).getOrElse { error ->
                return@post when (error) {
                    LoginError.InvalidCredentials -> call.respond(
                        HttpStatusCode.Unauthorized,
                        LoginResponse.InvalidCredentials(),
                    )

                    LoginError.InternalError -> call.respond(
                        HttpStatusCode.InternalServerError,
                        LoginResponse.ConnectionError(),
                    )
                }
            }

            val jwt = jwtProcessor.generateJwt(MemberIdClaim claimedAs memberId.value)

            call.respond(
                HttpStatusCode.OK,
                LoginResponse.Successful(
                    memberId = memberId,
                    token = jwt,
                ),
            )
        }
    }
}