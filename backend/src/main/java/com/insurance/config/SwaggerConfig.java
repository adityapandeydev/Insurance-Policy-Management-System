package com.insurance.config;

import io.swagger.v3.oas.annotations.OpenAPIDefinition;
import io.swagger.v3.oas.annotations.enums.SecuritySchemeType;
import io.swagger.v3.oas.annotations.info.Contact;
import io.swagger.v3.oas.annotations.info.Info;
import io.swagger.v3.oas.annotations.info.License;
import io.swagger.v3.oas.annotations.security.SecurityScheme;
import io.swagger.v3.oas.annotations.servers.Server;
import org.springframework.context.annotation.Configuration;

/**
 * ╔══════════════════════════════════════════════════════════════════════════╗
 * ║                   SWAGGER / OPENAPI CONFIGURATION                       ║
 * ╠══════════════════════════════════════════════════════════════════════════╣
 * ║                                                                          ║
 * ║  Configures the OpenAPI 3.0 specification for Swagger UI.               ║
 * ║                                                                          ║
 * ║  Swagger UI URL: http://localhost:8080/api/v1/swagger-ui.html           ║
 * ║  API Docs JSON: http://localhost:8080/api/v1/v3/api-docs               ║
 * ║                                                                          ║
 * ║  INTERVIEW TIP: OpenAPI vs Swagger                                      ║
 * ║  ─────────────────────────────────────────────────────────────────────  ║
 * ║  OpenAPI is the specification (open standard, previously called Swagger) ║
 * ║  Swagger is a suite of tools built around the OpenAPI spec.            ║
 * ║  SpringDoc generates the spec from annotations + Spring MVC metadata.  ║
 * ║                                                                          ║
 * ║  @SecurityScheme: Defines the JWT Bearer authentication scheme.         ║
 * ║  This adds the "Authorize" button in Swagger UI where testers can       ║
 * ║  enter their JWT token and test secured endpoints directly.             ║
 * ╚══════════════════════════════════════════════════════════════════════════╝
 */
@Configuration
@OpenAPIDefinition(
    info = @Info(
        title = "Insurance Policy Management System API",
        version = "1.0.0",
        description = """
            Enterprise Insurance Policy Management System
            
            ## Features
            - **Authentication**: JWT-based authentication with role-based access control
            - **Customer Management**: Full CRUD for customer profiles
            - **Policy Management**: Create, manage, and track insurance policies
            - **Claims Management**: Submit, review, approve, and reject claims
            - **Risk Assessment**: Automated customer risk scoring
            - **Dashboard**: Admin analytics and system metrics
            
            ## Authentication
            1. Register via `POST /auth/register` or Login via `POST /auth/login`
            2. Copy the `accessToken` from the response
            3. Click the **Authorize** button above and enter: `Bearer <your-token>`
            4. All secured endpoints will now include the token automatically
            
            ## Demo Credentials
            - Admin: `admin@insurance.com` / `Password123!`
            - Agent: `agent.sarah@insurance.com` / `Password123!`
            - Customer: `john.doe@email.com` / `Password123!`
            """,
        contact = @Contact(
            name = "Insurance System Support",
            email = "support@insurance.com",
            url = "https://insurance.com"
        ),
        license = @License(
            name = "MIT License",
            url = "https://opensource.org/licenses/MIT"
        )
    ),
    servers = {
        @Server(url = "/api/v1", description = "Local Development Server"),
        @Server(url = "https://api.insurance.com/api/v1", description = "Production Server")
    }
)
@SecurityScheme(
    name = "Bearer Authentication",
    type = SecuritySchemeType.HTTP,
    bearerFormat = "JWT",
    scheme = "bearer",
    description = "Enter JWT token obtained from /auth/login endpoint. Format: Bearer <token>"
)
public class SwaggerConfig {
    // No additional bean configuration needed.
    // SpringDoc auto-scans @RestController classes and @Operation annotations.
    // The @OpenAPIDefinition and @SecurityScheme annotations above are processed
    // by SpringDoc at startup to build the complete OpenAPI specification.
}
